import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * The interface defines an RMI server using Remote Connection.
 */
public interface RMIServer extends Remote {
  /**
   * The process request function processes client requests given by the client.
   * The type of requests which are acceptable are PUT,GET,DELETE
   * @param s It takes the string request starting with type of request followed by the data.
   * @return It returns server response
   * @throws RemoteException it throws a Remote exception if communication error occurs.
   */
  String processRequest(String s) throws RemoteException;

  /**
   * The function is used to register all replicate servers to the coordinator server.
   * @param coordinatorStub the coordinator server
   * @throws RemoteException it throws a Remote exception if communication error occurs.
   */
  void registerReplicaServer(RMIServer coordinatorStub) throws RemoteException;

  /**
   * The function is used in the first phase of 2PC protocol for PUT Request
   * @param key the key value for put
   * @param value the value for the key to be put in the data
   * @return returns boolean with status of first phase
   * @throws RemoteException it throws a Remote exception if communication error occurs.
   */
  boolean preparePut(String key, String value) throws RemoteException;

  /**
   * The function commits the Put requests after commit phase is complete in 2PC.
   * @param key the key for the data
   * @param value the value of the key
   * @throws RemoteException it throws a Remote exception if communication error occurs.
   */
  void doCommitPut(String key, String value) throws RemoteException;

  /**
   * The function updates the data to all replica servers once put is complete in one server.
   * @param newKeyValueStore the new data map to be replicated in other servers.
   * @throws RemoteException it throws a Remote exception if communication error occurs.
   */
  void updateKeyValueStore(Map<String, String> newKeyValueStore) throws RemoteException;

  /**
   * The function checks for the second phase for the Put requests in 2PC.
   * @param key the key for the data
   * @param value the value for the data
   * @return returns boolean with status for all server if ready for put.
   * @throws RemoteException it throws a Remote exception if communication error occurs.
   */
  boolean canCommitPut(String key, String value) throws RemoteException;

  /**
   * The function receives the response from prepare phase for put request.
   * @param key the key for the data
   * @param value the values for the data
   * @return returns boolean with the response
   * @throws RemoteException it throws a Remote exception if communication error occurs.
   */
  boolean receivePreparePutRequest(String key, String value) throws RemoteException;

  /**
   * The function receives the response after commit is done.
   * @param key the key value for the data
   * @param value the values for the data
   * @param b the boolean with the status of the put, true is put is success
   * @return boolean with the status of the put completion
   * @throws RemoteException it throws a Remote exception if communication error occurs.
   */
  boolean receivePreparePutResponse(String key, String value, boolean b) throws RemoteException;

  /**
   * The function checks for the second phase for the delete requests in 2PC.
   * @param key the key for the data
   * @return returns boolean with status for all server if ready for delete.
   * @throws RemoteException it throws a Remote exception if communication error occurs.
   */
  boolean canCommitDelete(String key) throws RemoteException;

  /**
   * The function receives the response from prepare phase for delete request.
   * @param key the key for the data
   * @return returns boolean with the response
   * @throws RemoteException it throws a Remote exception if communication error occurs.
   */
  boolean receivePrepareDeleteRequest(String key) throws RemoteException;

  /**
   * The function commits the delete requests after commit phase is complete in 2PC.
   * @param key the key for the data
   * @throws RemoteException it throws a Remote exception if communication error occurs.
   */
  void doCommitDelete(String key) throws RemoteException;

  /**
   * The function receives the response after delete is done.
   * @param key the key value for the data
   * @param canCommit the boolean with the status of delete, true is delete is success
   * @return boolean with the status of the put completion
   * @throws RemoteException it throws a Remote exception if communication error occurs.
   */
  boolean receivePrepareDeleteResponse(String key, boolean canCommit)
          throws RemoteException;

  /**
   * The function receives a message acknowledgement for in 2PC protocol.
   * @param msg type of request which can be a put or delete request
   * @param key the key for data
   * @param value  the values for the data
   * @return returns boolean with the success or failure of message acknowledgement
   * @throws RemoteException it throws a Remote exception if communication error occurs.
   */
  boolean receiveMessageWithACK(String msg, String key, String value) throws RemoteException;

  /**
   * The function is used in the first phase of 2PC protocol for DELETE Request
   * @param key the key value for delete
   * @return returns boolean with status of first phase
   * @throws RemoteException it throws a Remote exception if communication error occurs.
   */
  boolean prepareDelete(String key) throws RemoteException;

  /**
   * The function is used to receive messages from other servers.
   * @param msg type of request which can be a put or delete request
   * @param key the key of the data
   * @throws RemoteException it throws a Remote exception if communication error occurs.
   */
  void receiveMessageWithoutACK(String msg, String key) throws RemoteException;

  /**
   * The function is used to unregister a server from the coordinator.
   * @param replicaServer The RMI Server object to be removed
   * @throws RemoteException it throws a Remote exception if communication error occurs.
   */
  void unregisterReplicaServer(RMIServer replicaServer) throws RemoteException;
}
