import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ServerImpl implements RMIServer {

  private static ArrayList<RMIServer> replicaStubs;
  private static List<Integer> ports;
  private Map<String, String> data;
  private ExecutorService executorService;
  private Set<RMIServer> replicaServers;
  private boolean isCoordinator;


  public ServerImpl() {
    data = new HashMap<>();
    data.put("1", "a");
    data.put("2", "b");
    data.put("3", "c");
    data.put("4", "d");
    data.put("5", "e");
    data.put("6", "f");
    executorService = Executors.newFixedThreadPool(10);
    replicaServers = new HashSet<>();
    isCoordinator = false;
    ports = new ArrayList<>();
    replicaStubs = new ArrayList<>();
  }

  public static void main(String args[]) {
    int numberOfServer = 5;
    ServerImpl coordinator = null;
    for (int i = 1; i <= numberOfServer; i++) {
      ServerImpl server = new ServerImpl();
      if (i == 1) {
        coordinator = server;
        coordinator.isCoordinator = true;
        System.out.println("Server " + i + " is the Coordinator.");
      }
      int port = 1234 + i;
      start(server, port, coordinator);
    }
  }

  private static void start(RMIServer server, int port, RMIServer coordinator) {
    try {
      RMIServer replicaStub = (RMIServer) UnicastRemoteObject.exportObject(server, port);
      Registry registry = null;
      try {
        registry = LocateRegistry.createRegistry(port);
      } catch (RemoteException e) {
        registry = LocateRegistry.getRegistry(port);
      }
      registry.rebind("RemoteInterface", replicaStub);
      System.out.println("Server started on port: " + port);

      if (coordinator == null) {
        System.out.println("Server " + port + " is the Coordinator.");
      } else {
        if (coordinator != server) {
          try {
            int coordinatorRegistryPort = 1234;
            for (int p : ports) {
              if (p == coordinatorRegistryPort) {
                RMIServer coordinatorStub = replicaStubs.get(
                        ports.indexOf(p));
                server.registerReplicaServer(coordinatorStub);
                break;
              }
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public synchronized void registerReplicaServer(RMIServer replicaServer) {
    replicaServers.add(replicaServer);
    if (replicaServers.size() == 1) {
      isCoordinator = true;
    }
  }

  @Override
  public String processRequest(String line) throws RemoteException {
    String[] split = line.split(" ");
    if (split[0].equalsIgnoreCase("PUT")) {
      try {
        if (!split[1].isBlank()) {
          String value = data.get(split[1]);
          if (value == null) {
            executorService.execute(() -> {
              data.put(split[1], split[2]);
            });
            return "PUT Request Success";
          } else {
            return "Key already present, PUT Unsuccesfull";
          }
        }
        if (split[1].isBlank()) {
          return "Empty key value given for Put,try again";
        }
        return "Put Request Success";
      } catch (Exception e) {
        return "Put request unsuccessful";
      }

    } else if (split[0].equalsIgnoreCase("GET")) {
      try {
        String value = data.get(split[1]);
        if (value != null) {
          return "The GET request for key " + split[1] + " gave response : " + value;
        } else {
          return "The GET request for key " + split[1] + " not found ";
        }
      } catch (Exception e) {
        return "GET request unsuccessful";
      }

    } else if (split[0].equalsIgnoreCase("DELETE")) {
      try {
        String value = data.remove(split[1]);
        if (value != null) {
          return "Deleted value: " + value + " for key: " + split[1];
        } else {
          return "Delete key not found";
        }
      } catch (Exception e) {
        return "DELETE request unsuccessful";
      }
    } else {
      return "Invalid Request to the server";
    }
  }

  @Override
  public boolean preparePut(String key, String value) throws RemoteException {
    if (canCommitPut(key, value)) {
      doCommitPut(key, value);
      return true;
    }
    return false;
  }

  @Override
  public void doCommitPut(String key, String value) throws RemoteException {
    boolean allCanCommit = true;
    for (RMIServer replica : replicaServers) {
      if (!replica.receivePreparePutRequest(key, value)) {
        allCanCommit = false;
        break;
      }
    }
    if (allCanCommit) {
      data.put(key, value);
      List<Boolean> ackResults = new ArrayList<>();
      for (RMIServer replica : replicaServers) {
        replica.updateKeyValueStore(new HashMap<>(data));
        ackResults.add(sendMessageWithACK(replica, "PUT", key, value));
      }
      boolean allResults = ackResults.stream().allMatch(result -> result);
      if (allResults) {
        System.out.println("PUT Success");
      } else {
        System.out.println("PUT Fail");
      }
    } else {
      System.out.println("PUT Fail");
    }

  }

  @Override
  public void updateKeyValueStore(Map<String, String> newKeyValueStore) {
    data = newKeyValueStore;
  }

  @Override
  public boolean canCommitPut(String key, String value) {
    boolean canCommit = !data.containsKey(key);
    return canCommit;
  }

  @Override
  public boolean receivePreparePutRequest(String key, String value) {
    return canCommitPut(key, value);
  }

  @Override
  public boolean receivePreparePutResponse(String key, String value, boolean canCommit) throws RemoteException {
    if (!canCommit) {
      return false;
    }
    for (RMIServer replica : replicaServers) {
      if (!sendMessageWithACK(replica, "PUT", key, value)) {
        return false;
      }
    }
    return true;
  }


  private boolean sendMessageWithACK(RMIServer replica, String msg, String key, String value) throws RemoteException {
    boolean ackReceived = replica.receiveMessageWithACK(msg, key, value);
    return ackReceived;
  }

  @Override
  public boolean prepareDelete(String key) throws RemoteException {
    if (canCommitDelete(key)) {
      doCommitDelete(key);

      return true;
    }
    return false;
  }

  @Override
  public boolean canCommitDelete(String key) throws RemoteException {
    boolean canCommit = data.containsKey(key);
    return canCommit;
  }

  @Override
  public boolean receivePrepareDeleteRequest(String key) throws RemoteException {
    return canCommitDelete(key);
  }


  @Override
  public synchronized void doCommitDelete(String key) throws RemoteException {
    boolean allCanCommit = true;
    for (RMIServer replica : replicaServers) {
      if (!replica.receivePrepareDeleteRequest(key)) {
        allCanCommit = false;
        break;
      }
    }
    if (allCanCommit) {
      data.remove(key);
      List<Boolean> ackResults = new ArrayList<>();
      for (RMIServer replica : replicaServers) {
        sendMessageWithACK(replica, "DELETE ", key, "");
      }
      boolean allResults = ackResults.stream().allMatch(result -> result);
      if (allResults) {
        System.out.println("DELETE Success");
      } else {
        System.out.println("DELETE Fail 1");
      }
    } else {
      System.out.println("DELETE Fail");
    }
  }

  @Override
  public synchronized boolean receivePrepareDeleteResponse(String key, boolean canCommit) throws RemoteException {
    if (!canCommit) {
      return false;
    }
    for (RMIServer replica : replicaServers) {
      if (!sendMessageWithACK(replica, "DELETE", key, " ")) {
        System.out.println("here");
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean receiveMessageWithACK(String msg, String key, String value) {
    if (msg.equalsIgnoreCase("PUT")) {
      data.put(key, value);
      return true;
    } else if (msg.equalsIgnoreCase("DELETE")) {
      data.remove(key);
      System.out.println("Deleting key");
      return true;
    }
    return false;
  }

  private void sendMessageWithoutACK(RMIServer replica, String message, String key)
          throws RemoteException {
    replica.receiveMessageWithoutACK(message, key);
  }


  public void receiveMessageWithoutACK(String msg, String key) throws RemoteException {
    if (msg.equalsIgnoreCase("DELETE")) {
      data.remove(key);
    }
  }

  @Override
  public synchronized void unregisterReplicaServer(RMIServer replicaServer) {
    replicaServers.remove(replicaServer);
    //System.out.println("Unregistered replica server");
    if (replicaServers.size() == 0) {
      isCoordinator = false;
    }
  }


}
