package splitter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.v1.*;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SplitNodeTest {
    private static final Config driverConfig = Config.build().withoutEncryption().toConfig();
    private ServerControls embeddedDatabaseServer;

    @BeforeAll
    void initializeNeo4j() {

        this.embeddedDatabaseServer = TestServerBuilders
                .newInProcessBuilder()
                .withProcedure(SplitNode.class)
                .newServer();
    }

    @Test
    public void splitNodeTest() {
        try(Driver driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI(), driverConfig);
            Session session = driver.session()){
            session.run("CREATE (n1:A) SET n1.TestId = 1\n" +
                    "        CREATE (n2:A) SET n2.TestId = 2\n" +
                    "        CREATE (n3:A) SET n1.TestId = 3\n" +
                    "        CREATE (m:B)\n" +
                    "        CREATE (n2)-[r1:Rel]->(n1) SET r1.TestId = 101\n" +
                    "        CREATE (n3)-[r2:Rel]->(n1) SET r2.TestId = 102\n" +
                    "        CREATE (n1)-[r3:Rel]->(n2) SET r3.TestId = 201\n" +
                    "        CREATE (n1)-[r4:Rel]->(n3) SET r4.TestId = 202\n" +
                    "        CREATE (b:B)\n" +
                    "        CREATE (n1)-[r5:OtherRel]->(b) SET r5.TestId = 301");

            session.run("MATCH (n:A) where n.TestId = 1 WITH collect(n) as nodes CALL splitter.splitNodes(nodes, {startIndex: 0, indexProperty: \"SplitId\", relationshipTypes: [\"Rel\"]}) YIELD node RETURN node");

            List<Record> records = session.run("MATCH (n:A) where n.TestId = 1 return n").list();
            assertThat(records.size() == 6);
        }
    }
}
