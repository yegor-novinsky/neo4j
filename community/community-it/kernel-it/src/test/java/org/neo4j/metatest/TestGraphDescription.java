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
package org.neo4j.metatest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.GraphDescription;
import org.neo4j.test.GraphDescription.Graph;
import org.neo4j.test.GraphDescription.NODE;
import org.neo4j.test.GraphDescription.PROP;
import org.neo4j.test.GraphHolder;
import org.neo4j.test.TestData;
import org.neo4j.test.TestGraphDatabaseFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.neo4j.graphdb.Label.label;

public class TestGraphDescription implements GraphHolder
{
    private static GraphDatabaseService graphdb;
    @Rule
    public TestData<Map<String,Node>> data = TestData.producedThrough( GraphDescription.createGraphFor( this, true ) );

    @Test
    public void havingNoGraphAnnotationCreatesAnEmptyDataCollection()
    {
        assertTrue( "collection was not empty", data.get().isEmpty() );
    }

    @Test
    @Graph( "I know you" )
    public void canCreateGraphFromSingleString()
    {
        verifyIKnowYou( "know", "I" );
    }

    @Test
    @Graph( { "a TO b", "b TO c", "c TO a" } )
    public void canCreateGraphFromMultipleStrings()
    {
        Map<String,Node> graph = data.get();
        Set<Node> unique = new HashSet<>();
        Node n = graph.get( "a" );
        while ( unique.add( n ) )
        {
            try ( Transaction ignored = graphdb.beginTx() )
            {
                n = n.getSingleRelationship( RelationshipType.withName( "TO" ), Direction.OUTGOING ).getEndNode();
            }
        }
        assertEquals( graph.size(), unique.size() );
    }

    @Test
    @Graph( { "a:Person EATS b:Banana" } )
    public void ensurePeopleCanEatBananas()
    {
        Map<String,Node> graph = data.get();
        Node a = graph.get( "a" );
        Node b = graph.get( "b" );

        try ( Transaction ignored = graphdb.beginTx() )
        {
            assertTrue( a.hasLabel( label( "Person" ) ) );
            assertTrue( b.hasLabel( label( "Banana" ) ) );
        }
    }

    @Test
    @Graph( { "a:Person EATS b:Banana", "a EATS b:Apple" } )
    public void ensurePeopleCanEatBananasAndApples()
    {
        Map<String,Node> graph = data.get();
        Node a = graph.get( "a" );
        Node b = graph.get( "b" );

        try ( Transaction ignored = graphdb.beginTx() )
        {
            assertTrue( "Person label missing", a.hasLabel( label( "Person" ) ) );
            assertTrue( "Banana label missing", b.hasLabel( label( "Banana" ) ) );
            assertTrue( "Apple label missing", b.hasLabel( label( "Apple" ) ) );
        }
    }

    @Graph( value = { "I know you" }, nodes = { @NODE( name = "I", properties = { @PROP( key = "name", value = "me" ) } ) } )
    private void verifyIKnowYou( String type, String myName )
    {
        try ( Transaction ignored = graphdb.beginTx() )
        {
            Map<String, Node> graph = data.get();
            assertEquals( "Wrong graph size.", 2, graph.size() );
            Node iNode = graph.get( "I" );
            assertNotNull( "The node 'I' was not defined", iNode );
            Node you = graph.get( "you" );
            assertNotNull( "The node 'you' was not defined", you );
            assertEquals( "'I' has wrong 'name'.", myName, iNode.getProperty( "name" ) );
            assertEquals( "'you' has wrong 'name'.", "you",
                    you.getProperty( "name" ) );

            Iterator<Relationship> rels = iNode.getRelationships().iterator();
            assertTrue( "'I' has too few relationships", rels.hasNext() );
            Relationship rel = rels.next();
            assertEquals( "'I' is not related to 'you'", you, rel.getOtherNode( iNode ) );
            assertEquals( "Wrong relationship type.", type, rel.getType().name() );
            assertFalse( "'I' has too many relationships", rels.hasNext() );

            rels = you.getRelationships().iterator();
            assertTrue( "'you' has too few relationships", rels.hasNext() );
            rel = rels.next();
            assertEquals( "'you' is not related to 'i'", iNode, rel.getOtherNode( you ) );
            assertEquals( "Wrong relationship type.", type, rel.getType().name() );
            assertFalse( "'you' has too many relationships", rels.hasNext() );

            assertEquals( "wrong direction", iNode, rel.getStartNode() );
        }
    }

    @BeforeClass
    public static void startDatabase()
    {
        graphdb = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase();
    }

    @AfterClass
    public static void stopDatabase()
    {
        if ( graphdb != null )
        {
            graphdb.shutdown();
        }
        graphdb = null;
    }

    @Override
    public GraphDatabaseService graphdb()
    {
        return graphdb;
    }
}