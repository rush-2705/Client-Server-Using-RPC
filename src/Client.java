import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


/**
 * The class defines a client.
 */
public class Client {
  /**
   * The main function runs the client program for interaction.
   * It provides the user with options to connect with server data.
   * @param args No string command line arguments are required.
   */
  public static void main(String args[]) {
    Scanner sc = new Scanner(System.in);
    try {

      int numReplicas = 5;

      List<RMIServer> replicaStubs = new ArrayList<>();
      List<Integer> replicaRegistryPorts = new ArrayList<>();
      for (int i = 1; i <= numReplicas; i++) {
        int registryPort = 1234 + i;
        RMIServer replicaStub = connectToReplica(registryPort);
        if (replicaStub != null) {
          replicaStubs.add(replicaStub);
          replicaRegistryPorts.add(registryPort);
        }
      }

      RMIServer coordinatorStub = replicaStubs.get(0);
      for (int i = 1; i < replicaStubs.size(); i++) {
        coordinatorStub.registerReplicaServer(replicaStubs.get(i));
      }
      int value = 1;
      while (value != 4) {
        System.out.println(" Choose an option from the menu");
        System.out.println("1. Put(key,value)");
        System.out.println("2. Get value for a key");
        System.out.println("3. Delete value for a key");
        System.out.println("4. Exit");
        try {
          value = sc.nextInt();
          sc.nextLine();
        } catch (Exception e) {
          System.out.println(" Enter an integer value");
          continue;
        }
        switch (value) {
          case 1:
            System.out.println("Enter Server number to contact (1-5");
            int n=sc.nextInt();
            sc.nextLine();
            if(n<1 || n>5){
              System.out.println("Invalid Server number.");
              break;
            }
            System.out.println("Enter the key");
            String key = sc.nextLine();
            System.out.println("Enter value");
            String val = sc.nextLine();
            if(coordinatorStub.preparePut(key, val)){
              if(coordinatorStub.receivePreparePutResponse(key, val, true)){
                System.out.println("PUT Success");
              }else {
                System.out.println("PUT Failed");
              }
            }else {
              System.out.println("PUT Failed");
            }
            break;
          case 2:
            System.out.println("Enter Server number to contact (1-5");
            int number=sc.nextInt();
            sc.nextLine();
            if(number<1 || number>5){
              System.out.println("Invalid Server number.");
              break;
            }
            RMIServer getServer = replicaStubs.get(number - 1);
            System.out.println("Enter the key");
            String get = sc.nextLine();
            System.out.println("Client Sending Values");
            String put = getServer.processRequest("GET " + get + " " + "\n");
            System.out.println(" Server Response");
            System.out.println(put);
            break;
          case 3:
            System.out.println("Enter Server number to contact (1-5");
            int n1=sc.nextInt();
            sc.nextLine();
            if(n1<1 || n1>5){
              System.out.println("Invalid Server number.");
              break;
            }
            System.out.println("Enter the key");
            String del = sc.nextLine();
            boolean r=coordinatorStub.prepareDelete(del);
            if (r) {
              boolean r2=coordinatorStub.receivePrepareDeleteResponse(del, true);
              if (r2) {
                System.out.println("DELETE Success");
              } else {
                System.out.println("DELETE Fail");
              }
            } else {
              System.out.println("DELETE Fail");
            }
            break;
          case 4:
            System.out.println("Client Exit");
            break;
          default:
            System.out.println("Invalid Input");
        }
      }
      sc.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static RMIServer connectToReplica(int registryPort) {
    try {
      String name = "localhost";
      Registry registry = LocateRegistry.getRegistry(name, registryPort);
      RMIServer replicaStub = (RMIServer) registry.lookup("RemoteInterface");
      return replicaStub;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
