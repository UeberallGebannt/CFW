package de.cuuky.cfw.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.cuuky.cfw.mysql.request.PreparedStatementHandler;

public class MySQLClient {

	private static final ExecutorService THREAD_POOL;

	static {
		THREAD_POOL = Executors.newCachedThreadPool();
	}

	protected Connection connection;
	protected String host, database, user, password;
	protected int port;
	protected Object connectWait;
	protected boolean autoReconnect;

	private volatile CopyOnWriteArrayList<MySQLRequest> queries;

	public MySQLClient(String host, int port, String database, String user, String password) {
		this.host = host;
		this.port = port;
		this.database = database;
		this.user = user;
		this.password = password;
		this.autoReconnect = true;
		this.queries = new CopyOnWriteArrayList<MySQLRequest>();

		startConnecting();
		THREAD_POOL.execute(this::prepareAsyncHandler);
	}

	public MySQLClient(String host, int port, String database, String user, String password, Object connectWait) {
		this(host, port, database, user, password);

		this.connectWait = connectWait;
	}

	private void startConnecting() {
		THREAD_POOL.execute(() -> {
			try {
				this.connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?allowMultiQueries=true&autoReconnect=true&testWhileIdle=true&testOnBorrow=true", user, password);

				if (connectWait != null) {
					synchronized (this.connectWait) {
						this.connectWait.notifyAll();
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
				System.err.println("[MySQL] Couldn't connect to MySQL-Database!");
			}
		});
	}

	private boolean getQuery(MySQLRequest mqr) {
		this.waitForConnection();

		try {
			PreparedStatement statement = connection.prepareStatement(mqr.getSql());
			if (mqr.getHandler() != null)
				mqr.getHandler().onStatementPrepared(statement);
			mqr.doRequest(statement);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("[MySQL] An error occured on executing a query!");
			System.err.println("[MySQL] Query: " + mqr.getSql());
			return false;
		}

		return true;
	}

	private Runnable prepareAsyncHandler() {
		while (true) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			waitForConnection();

			MySQLRequest[] loop = queries.toArray(new MySQLRequest[0]);
			for (int i = loop.length - 1; i >= 0; i--) {
				MySQLRequest mqr = loop[i];
				queries.remove(mqr);
				THREAD_POOL.execute(() -> {
					if (!getQuery(mqr))
						queries.add(mqr);
				});
			}
		}
	}

	protected void waitForConnection() {
		if (isConnected())
			return;

		synchronized (this.connectWait) {
			try {
				this.connectWait.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void disconnect() {
		if (!isConnected())
			return;

		try {
			this.connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		this.connection = null;
	}

	public boolean getQuery(String query, boolean async) {
		return getQuery(query, null, async);
	}

	public boolean getQuery(String query, PreparedStatementHandler handler, boolean async) {
		return async ? this.queries.add(new MySQLRequest(query, handler)) : getQuery(new MySQLRequest(query, handler));
	}

	public boolean getQuery(String query) {
		return getQuery(new MySQLRequest(query, null));
	}

	public boolean getQuery(String query, PreparedStatementHandler handler) {
		return getQuery(new MySQLRequest(query, handler));
	}

	public boolean getAsyncPreparedQuery(String query) {
		return this.queries.add(new MySQLRequest(query, null));
	}

	public boolean getAsyncPreparedQuery(String query, PreparedStatementHandler dr) {
		return this.queries.add(new MySQLRequest(query, dr));
	}

	public boolean isConnected() {
		return this.connection != null;
	}
}