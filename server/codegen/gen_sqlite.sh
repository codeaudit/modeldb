rm -rf jars/
rm sqlite/*jar
mkdir -p jars
wget http://central.maven.org/maven2/org/jooq/jooq/3.8.4/jooq-3.8.4.jar -O jars/jooq-3.8.4.jar
wget http://central.maven.org/maven2/org/jooq/jooq-meta/3.8.4/jooq-meta-3.8.4.jar -O jars/jooq-meta-3.8.4.jar
wget http://central.maven.org/maven2/org/jooq/jooq-codegen/3.8.4/jooq-codegen-3.8.4.jar -O jars/jooq-codegen-3.8.4.jar
wget http://central.maven.org/maven2/org/xerial/sqlite-jdbc/3.8.11.2/sqlite-jdbc-3.8.11.2.jar -O sqlite/sqlite-jdbc-3.8.11.2.jar
cat ./sqlite/createDb.sql | sqlite3 modeldb.db  &&
java -classpath jars/jooq-3.8.4.jar:jars/jooq-meta-3.8.4.jar:jars/jooq-codegen-3.8.4.jar:sqlite/sqlite-jdbc-3.8.11.2.jar:. org.jooq.util.GenerationTool sqlite/library.xml && 
mv modeldb.db ../