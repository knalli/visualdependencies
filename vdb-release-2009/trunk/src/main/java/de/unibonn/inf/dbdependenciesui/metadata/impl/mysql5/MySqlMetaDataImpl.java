/**
 * $Id: Main.java 41 2009-03-01 16:21:20Z philipp $
 */
package de.unibonn.inf.dbdependenciesui.metadata.impl.mysql5;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import de.unibonn.inf.dbdependenciesui.hibernate.HibernateDAOFactory;
import de.unibonn.inf.dbdependenciesui.hibernate.models.DatabaseConnection;
import de.unibonn.inf.dbdependenciesui.hibernate.models.DatabaseProcedure;
import de.unibonn.inf.dbdependenciesui.hibernate.models.DatabaseTable;
import de.unibonn.inf.dbdependenciesui.hibernate.models.DatabaseTrigger;
import de.unibonn.inf.dbdependenciesui.hibernate.models.DatabaseView;
import de.unibonn.inf.dbdependenciesui.metadata.IConnectionBuilder;
import de.unibonn.inf.dbdependenciesui.metadata.MetaDataFactory.Vendor;
import de.unibonn.inf.dbdependenciesui.metadata.impl.AbstractConnectionBuilder;
import de.unibonn.inf.dbdependenciesui.metadata.impl.AbstractMetaData;

/**
 * This mysql implementation retrives several meta information about a given database connection.
 * 
 * @author Andre Kasper
 * @author Jan Philipp
 * @author Marc Kastleiner
 * @since 1.3
 */
public class MySqlMetaDataImpl extends AbstractMetaData {

	private final List<DatabaseTable> tables = new ArrayList<DatabaseTable>();

	private final List<DatabaseView> views = new ArrayList<DatabaseView>();

	private final List<DatabaseTrigger> triggers = new ArrayList<DatabaseTrigger>();

	private final List<DatabaseProcedure> procedures = new ArrayList<DatabaseProcedure>();

	public MySqlMetaDataImpl() {
		super(Vendor.MYSQL);
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (final ClassNotFoundException e) {
			handleException(e);
		}
	}

	@Override
	public int getNumberOfTables() {
		initializeIfRequired();
		return tables.size();
	}

	@Override
	public int getNumberOfTriggers() {
		initializeIfRequired();
		return triggers.size();
	}

	@Override
	public int getNumberOfProcedures() {
		initializeIfRequired();
		return procedures.size();
	}

	@Override
	public int getNumberOfViews() {
		initializeIfRequired();
		return views.size();
	}

	@Override
	public void analyze() {
		tables.clear();
		views.clear();
		triggers.clear();

		if (isReplaceMode()) {
			log.info("All objects of connection " + databaseConnection + " will be erased.");
			databaseConnection.clearObjects();
			HibernateDAOFactory.getSession().flush();
		}

		try {
			analyzer.analyze(connection, databaseConnection);
		} catch (final SQLException e) {
			handleException(e);
			e.printStackTrace();
		}

		tables.addAll(analyzer.getTables());
		views.addAll(analyzer.getViews());
		triggers.addAll(analyzer.getTriggers());
	}

	@Override
	public void parseViews() {
		for (final DatabaseView view : views) {
			viewParser.parse(tables, views, view);
		}
	}

	@Override
	public void parseTriggers() {
		final List<DatabaseTable> elements = new ArrayList<DatabaseTable>();
		elements.addAll(tables);
		elements.addAll(views);
		for (final DatabaseTrigger trigger : triggers) {
			triggerParser.parse(trigger);
		}
	}

	@Override
	public void parseProcedures() {
	// final List<DatabaseTable> elements = new ArrayList<DatabaseTable>();
	// elements.addAll(tables);
	// elements.addAll(views);
	// for (final DatabaseTrigger trigger : triggers) {
	// triggerParser.parse(trigger);
	// }
	}

	@Override
	public String createUrl(final String host, final int port, final String database) {
		return "jdbc:mysql://" + host + ":" + port + "/" + database;
	}

	@Override
	public IConnectionBuilder getConnectionBuilder() {
		return new MySqlConnection();
	}

	// @Override
	// public String getDescription() {
	// return Internationalization.getText("plugins.metadata.mysql.common");
	// }

	public class MySqlConnection extends AbstractConnectionBuilder {
		@Override
		public Connection createConnection(final DatabaseConnection connection) throws SQLException {
			final String url = createUrl(connection.getHost(), connection.getPort(), connection.getDatabase());
			final Properties properties = new Properties();
			properties.put("user", connection.getUsername());
			properties.put("password", connection.getPassword());
			// if (connection.getSysdba()) {
			// properties.put("internal_logon", "sysdba");
			// }
			return createConnection(url, properties);
		}
	}
}
