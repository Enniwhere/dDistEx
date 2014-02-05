import java.rmi.*;

public interface Echo extends Remote {
    public String echo(String message) throws RemoteException;
}