/*
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.core;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Iterator;
import java.util.Map;

import org.neo4j.graphdb.ConstraintViolationException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.rule.DbmsRule;
import org.neo4j.test.rule.ImpermanentDbmsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NodeTest
{
    @ClassRule
    public static DbmsRule db = new ImpermanentDbmsRule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldGiveHelpfulExceptionWhenDeletingNodeWithRels()
    {
        // Given
        Node node;

        try ( Transaction transaction = db.beginTx() )
        {
            node = db.createNode();
            Node node2 = db.createNode();
            node.createRelationshipTo( node2, RelationshipType.withName( "MAYOR_OF" ) );
            transaction.success();
        }

        // And given a transaction deleting just the node
        try ( Transaction transaction = db.beginTx() )
        {
            node.delete();

            // Expect
            exception.expect( ConstraintViolationException.class );
            exception.expectMessage( "Cannot delete node<" + node.getId() + ">, because it still has relationships. " +
                    "To delete this node, you must first delete its relationships." );

            // When I commit
            transaction.success();
        }
    }

    @Test
    public void testNodeCreateAndDelete()
    {
        long nodeId;
        try ( Transaction transaction = db.beginTx() )
        {
            Node node = getGraphDb().createNode();
            nodeId = node.getId();
            getGraphDb().getNodeById( nodeId );
            node.delete();
            transaction.success();
        }
        try ( Transaction transaction = db.beginTx() )
        {
            getGraphDb().getNodeById( nodeId );
            fail( "Node[" + nodeId + "] should be deleted." );
        }
        catch ( NotFoundException ignored )
        {
        }
    }

    @Test
    public void testDeletedNode()
    {
        // do some evil stuff
        try ( Transaction transaction = db.beginTx() )
        {
            Node node = getGraphDb().createNode();
            node.delete();
            try
            {
                node.setProperty( "key1", 1 );
                fail( "Adding stuff to deleted node should throw exception" );
            }
            catch ( Exception e )
            { // good
            }
        }
    }

    @Test
    public void testNodeAddPropertyWithNullKey()
    {
        try ( Transaction transaction = db.beginTx() )
        {
            Node node1 = getGraphDb().createNode();
            try
            {
                node1.setProperty( null, "bar" );
                fail( "Null key should result in exception." );
            }
            catch ( IllegalArgumentException ignored )
            {
            }
        }
    }

    @Test
    public void testNodeAddPropertyWithNullValue()
    {
        try ( Transaction transaction = db.beginTx() )
        {
            Node node1 = getGraphDb().createNode();
            try
            {
                node1.setProperty( "foo", null );
                fail( "Null value should result in exception." );
            }
            catch ( IllegalArgumentException ignored )
            {
            }
            transaction.failure();
        }
    }

    @Test
    public void testNodeAddProperty()
    {
        try ( Transaction transaction = db.beginTx() )
        {
            Node node1 = getGraphDb().createNode();
            Node node2 = getGraphDb().createNode();

            String key1 = "key1";
            String key2 = "key2";
            String key3 = "key3";
            Integer int1 = 1;
            Integer int2 = 2;
            String string1 = "1";
            String string2 = "2";

            // add property
            node1.setProperty( key1, int1 );
            node2.setProperty( key1, string1 );
            node1.setProperty( key2, string2 );
            node2.setProperty( key2, int2 );
            assertTrue( node1.hasProperty( key1 ) );
            assertTrue( node2.hasProperty( key1 ) );
            assertTrue( node1.hasProperty( key2 ) );
            assertTrue( node2.hasProperty( key2 ) );
            assertTrue( !node1.hasProperty( key3 ) );
            assertTrue( !node2.hasProperty( key3 ) );
            assertEquals( int1, node1.getProperty( key1 ) );
            assertEquals( string1, node2.getProperty( key1 ) );
            assertEquals( string2, node1.getProperty( key2 ) );
            assertEquals( int2, node2.getProperty( key2 ) );
        }
    }

    @Test
    public void testNodeRemoveProperty()
    {
        try ( Transaction transaction = db.beginTx() )
        {
            String key1 = "key1";
            String key2 = "key2";
            Integer int1 = 1;
            Integer int2 = 2;
            String string1 = "1";
            String string2 = "2";

            Node node1 = getGraphDb().createNode();
            Node node2 = getGraphDb().createNode();

            try
            {
                if ( node1.removeProperty( key1 ) != null )
                {
                    fail( "Remove of non existing property should return null" );
                }
            }
            catch ( NotFoundException ignored )
            {
            }
            try
            {
                node1.removeProperty( null );
                fail( "Remove null property should throw exception." );
            }
            catch ( IllegalArgumentException ignored )
            {
            }

            node1.setProperty( key1, int1 );
            node2.setProperty( key1, string1 );
            node1.setProperty( key2, string2 );
            node2.setProperty( key2, int2 );
            try
            {
                node1.removeProperty( null );
                fail( "Null argument should result in exception." );
            }
            catch ( IllegalArgumentException ignored )
            {
            }

            // test remove property
            assertEquals( int1, node1.removeProperty( key1 ) );
            assertEquals( string1, node2.removeProperty( key1 ) );
            // test remove of non existing property
            try
            {
                if ( node2.removeProperty( key1 ) != null )
                {
                    fail( "Remove of non existing property return null." );
                }
            }
            catch ( NotFoundException e )
            {
                // must mark as rollback only
            }
        }
    }

    @Test
    public void testNodeChangeProperty()
    {
        try ( Transaction transaction = db.beginTx() )
        {
            String key1 = "key1";
            String key2 = "key2";
            String key3 = "key3";
            Integer int1 = 1;
            Integer int2 = 2;
            String string1 = "1";
            String string2 = "2";
            Boolean bool1 = Boolean.TRUE;
            Boolean bool2 = Boolean.FALSE;

            Node node1 = getGraphDb().createNode();
            Node node2 = getGraphDb().createNode();
            node1.setProperty( key1, int1 );
            node2.setProperty( key1, string1 );
            node1.setProperty( key2, string2 );
            node2.setProperty( key2, int2 );

            try
            {
                node1.setProperty( null, null );
                fail( "Null argument should result in exception." );
            }
            catch ( IllegalArgumentException ignored )
            {
            }
            catch ( NotFoundException e )
            {
                fail( "wrong exception" );
            }

            // test change property
            node1.setProperty( key1, int2 );
            node2.setProperty( key1, string2 );
            assertEquals( string2, node2.getProperty( key1 ) );
            node1.setProperty( key3, bool1 );
            node1.setProperty( key3, bool2 );
            assertEquals( string2, node2.getProperty( key1 ) );
            node1.delete();
            node2.delete();
        }
    }

    @Test
    public void testNodeChangeProperty2()
    {
        try ( Transaction transaction = db.beginTx() )
        {
            String key1 = "key1";
            Integer int1 = 1;
            Integer int2 = 2;
            String string1 = "1";
            String string2 = "2";
            Boolean bool1 = Boolean.TRUE;
            Boolean bool2 = Boolean.FALSE;
            Node node1 = getGraphDb().createNode();
            node1.setProperty( key1, int1 );
            node1.setProperty( key1, int2 );
            assertEquals( int2, node1.getProperty( key1 ) );
            node1.removeProperty( key1 );
            node1.setProperty( key1, string1 );
            node1.setProperty( key1, string2 );
            assertEquals( string2, node1.getProperty( key1 ) );
            node1.removeProperty( key1 );
            node1.setProperty( key1, bool1 );
            node1.setProperty( key1, bool2 );
            assertEquals( bool2, node1.getProperty( key1 ) );
            node1.removeProperty( key1 );
            node1.delete();
        }
    }

    @Test
    public void testNodeGetProperties()
    {
        try ( Transaction transaction = db.beginTx() )
        {
            String key1 = "key1";
            String key2 = "key2";
            String key3 = "key3";
            Integer int1 = 1;
            Integer int2 = 2;
            String string = "3";

            Node node1 = getGraphDb().createNode();
            try
            {
                node1.getProperty( key1 );
                fail( "get non existing property didn't throw exception" );
            }
            catch ( NotFoundException ignored )
            {
            }
            try
            {
                node1.getProperty( null );
                fail( "get of null key didn't throw exception" );
            }
            catch ( IllegalArgumentException ignored )
            {
            }
            assertTrue( !node1.hasProperty( key1 ) );
            assertTrue( !node1.hasProperty( null ) );
            node1.setProperty( key1, int1 );
            node1.setProperty( key2, int2 );
            node1.setProperty( key3, string );
            Iterator<String> keys = node1.getPropertyKeys().iterator();
            keys.next();
            keys.next();
            keys.next();
            Map<String,Object> properties = node1.getAllProperties();
            assertEquals( properties.get( key1 ), int1 );
            assertEquals( properties.get( key2 ), int2 );
            assertEquals( properties.get( key3 ), string );
            properties = node1.getProperties( key1, key2 );
            assertEquals( properties.get( key1 ), int1 );
            assertEquals( properties.get( key2 ), int2 );
            assertFalse( properties.containsKey( key3 ) );

            properties = node1.getProperties();
            assertTrue( properties.isEmpty() );

            try
            {
                String[] names = null;
                node1.getProperties( names );
                fail();
            }
            catch ( NullPointerException e )
            {
                // Ok
            }

            try
            {
                String[] names = new String[]{null};
                node1.getProperties( names );
                fail();
            }
            catch ( NullPointerException e )
            {
                // Ok
            }

            try
            {
                node1.removeProperty( key3 );
            }
            catch ( NotFoundException e )
            {
                fail( "Remove of property failed." );
            }
            assertTrue( !node1.hasProperty( key3 ) );
            assertTrue( !node1.hasProperty( null ) );
            node1.delete();
        }
    }

    @Test
    public void testAddPropertyThenDelete()
    {
        Node node;
        try ( Transaction transaction = db.beginTx() )
        {
            node = getGraphDb().createNode();
            node.setProperty( "test", "test" );
            transaction.success();
        }
        try ( Transaction transaction = db.beginTx() )
        {
            node.setProperty( "test2", "test2" );
            node.delete();
            transaction.success();
        }
    }

    @Test
    public void testChangeProperty()
    {
        Node node;
        try ( Transaction transaction = db.beginTx() )
        {
            node = getGraphDb().createNode();
            node.setProperty( "test", "test1" );
            transaction.success();
        }
        try ( Transaction transaction = db.beginTx() )
        {
            node.setProperty( "test", "test2" );
            node.removeProperty( "test" );
            node.setProperty( "test", "test3" );
            assertEquals( "test3", node.getProperty( "test" ) );
            node.removeProperty( "test" );
            node.setProperty( "test", "test4" );
            transaction.success();
        }
        try ( Transaction transaction = db.beginTx() )
        {
            assertEquals( "test4", node.getProperty( "test" ) );
        }
    }

    @Test
    public void testChangeProperty2()
    {
        Node node;
        try ( Transaction transaction = db.beginTx() )
        {
            node = getGraphDb().createNode();
            node.setProperty( "test", "test1" );
            transaction.success();
        }
        try ( Transaction transaction = db.beginTx() )
        {
            node.removeProperty( "test" );
            node.setProperty( "test", "test3" );
            assertEquals( "test3", node.getProperty( "test" ) );
            transaction.success();
        }
        try ( Transaction transaction = db.beginTx() )
        {
            assertEquals( "test3", node.getProperty( "test" ) );
            node.removeProperty( "test" );
            node.setProperty( "test", "test4" );
            transaction.success();
        }
        try ( Transaction transaction = db.beginTx() )
        {
            assertEquals( "test4", node.getProperty( "test" ) );
        }
    }

    private GraphDatabaseService getGraphDb()
    {
        return db.getGraphDatabaseAPI();
    }
}