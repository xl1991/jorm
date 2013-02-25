# jORM

jORM is a lightweight Java ORM. It does not aim at solving every database problem. It primarily cures the boilerplatisis that many Java solutions suffer from, while exposing the functionality of the JDBC through a convenient interface.

## Getting started

### Getting the code

Getting jORM to a public maven repo is one of the items on the timeline of the project. For now clone the git repo to get a fresh copy!

    > git clone git://github.com/jajja/jorm.git
    > cd jorm
    > mvn install

Then include the dependency to jORM in any project you are working on that needs a lightweight ORM.

    <dependency>
        <groupId>com.jajja</groupId>
        <artifactId>jorm</artifactId>
        <version>1.0.0</version>
    </dependency>

Now that you've got the code, let's see if we cannot conjure some cheap tricks!

### Configuring database

The database abstraction in jORM needs a `javax.sql.DataSource` data source. One recommended implementation is the Apache Commons DBCP basic data source.

    BasicDataSource moriaDataSource = new BasicDataSource();
    moriaDataSource.setDriverClassName("org.postgresql.Driver");
    moriaDataSource.setUrl("jdbc:postgresql://localhost:5432/moria");
    moriaDataSource.setUsername("gandalf");
    moriaDataSource.setPassword("mellon");
    
    Database.configure("moria", moriaDataSource);

This will configure the pooled DBCP data source as a named database. For all of those who prefer Spring Beans this can be achieved through a singleton factory method.

    <bean id="moriaDataSource" class="org.apache.commons.dbcp.BasicDataSource">
        <property name="driverClassname" value="org.postgresql.Driver" />
        <property name="url" value="jdbc:postgresql://localhost:5432/moria" />
        <property name="username" value="gandalf" />
        <property name="password" value="mellon" />
    </bean>
    
    <bean class="com.jajja.jorm.Database" factory-method="get">
        <property name="dataSources">
            <map>
                <entry key="moria" value-ref="moriaDataSource" />
            </map>
        </property>
    </bean>

### Using databases

All database queries in jORM are executed through a thread local transaction. The first query begins the transaction. After that the transaction can be committed or closed, which implicitly rolls back the transaction.

    Transaction transaction = Database.open("moria");
    try {
        transaction.select("UPDATE goblins SET mindset = 'provoked' RETURNING *");
        transaction.commit();
    } catch (SQLException e) {
        // handle e
    } finally {
        transaction.close();
    }

The database has a shorthand to the thread local transactions. The above can also be expressed as below.

    try {
        Database.open("moria").select("UPDATE goblins SET mindset = 'provoked' RETURNING *");
        Database.commit("moria");
    } catch (SQLException e) {
        // handle e
    } finally {
        Database.close("moria");
    }

If you are using multiple databases it may be a good idea to close all thread local transactions at the end of execution. This can be done by a single call.

    Database.close();
    
Maybe you where interested in something more than executing generic queries. Let's map a table!

### Mapping tables

In order to map a table we need to get an idea of how it is declared. Imagine a table was created using the following statement.

    CREATE TABLE goblin (
        id          serial    NOT NULL,
        tribe_id    int       NOT NULL    REFERENCES tribes(id),
        name        varchar   NOT NULL    DEFAULT 'Azog', 
        mindset     varchar,
        PRIMARY KEY (id),
        UNIQUE (tribe_id, name)
    );

Tables are mapped by records with a little help by the `@Jorm` annotation. Records bind to the tread local transactions defined by the `database` attribute. The `table` attribute defines the mapped table, and the `id` attribute provides public key functionalite like `Record#findById(Class<? extends Record, Object)`.

    @Jorm(database="moria", table="goblins", id="id")
    public class Goblin extends Record {
    
        public Integer getId() {
            return get("id", Integer.class);
        }
    
        public void setId(Integer id) {
            set("id", id);
        }
    
        public Integer getTribeId() {
            return get("tribe_id", Integer.class);
        }
    
        public void setTribeId(Integer id) {
            set("tribe_id", id);
        }
    
        public Tribe getTribe() {
            return get("tribe_id", Tribe.class);
        }
    
        public void setTribe(Tribe tribe) {
            set("tribe_id", tribe);
        }
    
        public String getName() {
            return get("name", String.class);
        }
    
        public void setName(String name) {
            set("name", name);
        }
    
        public String getMindset() {
            return get("mindset", String.class);
        }
    
        public void setMindset(String mindset) {
            set("mindset", mindset);
        }
    
    }

Such records can be automatically generated by the `Generator` class. Note that the `Goblin#getTribe()` and `Goblin#setTribe()` methods reffers to the `tribe_id` field of the mapped `Goblin` record, but `Tribe` record is also cached for subsequent references. Thus foreign keys can be mapped, but how would a tribe look?

    @Jorm(database="moria", table="tribes", id="id")
    public class Tribe extends Record {
        
        public Integer getId() {
            return get("id", Integer.class);
        }
        
        public void setId(Integer id) {
            set("id", id);
        }
        
        public String getName() {
            return get("name", String.class);
        }
        
        public void setName(String name) {
            set("name", name);
        }
        
        public List<Goblin> getGoblins() throws SQLException {
            return findReferences(Goblin.class, "id");
        }
        
    }

There is no default implementation of `Tribe#setGoblins(List<Goblin>)`. This is not because it is impossible to implement using jORM, but because at this point jORM makes no claim at providing a proper cache for one-to-many relations. There is however a cache implementation for records that could be used for methods like `Tribe#getGoblins()`. For now we'll just let it use a query for each access, and we'll get back to caching strategies.

Did you notice the `UNIQUE` constraint on goblins? These can be used to provide convenience queries on goblins.

    public static Goblin findByTribeAndName(Tribe tribe, String name) throws SQLException {
        return find(Goblin.class, new Column("tribe_id", tribe), new Column("name", name));
    }

If you prefer the write SQL this can also be ahieved trough manual queries.

    public static Goblin findByTribeAndName(Tribe tribe, String name) throws SQLException {
        return Record.select(Goblin.class, "SELECT * FROM goblins WHERE tribe_id = #1:id# AND name = #2#", tribe, name);
    }

This should be where you've caught the glimpse of a tip of an iceberg, and should ask yourself. What else is there?

## To be continued..

This README will be updated with more advanced and in-depth examples of how to best make use of jORM. One of the first things on our TODO list is to document the SQL markup syntax for queries through records and transactions properly.
