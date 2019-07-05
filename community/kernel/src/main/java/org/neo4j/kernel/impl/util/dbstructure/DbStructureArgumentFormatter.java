/*
 * Copyright (c) 2002-2019 "Neo4j,"
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
package org.neo4j.kernel.impl.util.dbstructure;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.neo4j.internal.helpers.Strings;
import org.neo4j.internal.schema.IndexDescriptor;
import org.neo4j.internal.schema.IndexPrototype;
import org.neo4j.internal.schema.LabelSchemaDescriptor;
import org.neo4j.internal.schema.RelationTypeSchemaDescriptor;
import org.neo4j.internal.schema.SchemaDescriptor;
import org.neo4j.internal.schema.constraints.ConstraintDescriptorFactory;
import org.neo4j.internal.schema.constraints.NodeExistenceConstraintDescriptor;
import org.neo4j.internal.schema.constraints.NodeKeyConstraintDescriptor;
import org.neo4j.internal.schema.constraints.RelExistenceConstraintDescriptor;
import org.neo4j.internal.schema.constraints.UniquenessConstraintDescriptor;

import static java.lang.String.format;

public enum DbStructureArgumentFormatter implements ArgumentFormatter
{
    INSTANCE;

    private static List<String> IMPORTS = Arrays.asList(
            ConstraintDescriptorFactory.class.getCanonicalName(),
            UniquenessConstraintDescriptor.class.getCanonicalName(),
            RelExistenceConstraintDescriptor.class.getCanonicalName(),
            NodeExistenceConstraintDescriptor.class.getCanonicalName(),
            NodeKeyConstraintDescriptor.class.getCanonicalName(),
            SchemaDescriptor.class.getCanonicalName(),
            IndexDescriptor.class.getCanonicalName(),
            IndexPrototype.class.getCanonicalName()
    );

    @Override
    public Collection<String> imports()
    {
        return IMPORTS;
    }

    @Override
    public void formatArgument( Appendable builder, Object arg ) throws IOException
    {
        if ( arg == null )
        {
            builder.append( "null" );
        }
        else if ( arg instanceof String )
        {
            builder.append( '"' );
            Strings.escape( builder, arg.toString() );
            builder.append( '"' );
        }
        else if ( arg instanceof Long )
        {
            builder.append( arg.toString() );
            builder.append( 'L' );
        }
        else if ( arg instanceof Integer )
        {
            builder.append( arg.toString() );
        }
        else if ( arg instanceof Double )
        {
            double d = (Double) arg;
            if ( Double.isNaN( d ) )
            {
                builder.append( "Double.NaN" );
            }
            else if ( Double.isInfinite( d ) )
            {
                builder.append( d < 0 ? "Double.NEGATIVE_INFINITY" : "Double.POSITIVE_INFINITY" );
            }
            else
            {
                builder.append( arg.toString() );
                builder.append( 'd' );
            }
        }
        else if ( arg instanceof IndexDescriptor )
        {
            IndexDescriptor descriptor = (IndexDescriptor) arg;
            String className = IndexPrototype.class.getSimpleName();
            SchemaDescriptor schema = descriptor.schema();
            String methodName = !descriptor.isUnique() ? "forSchema" : "uniqueForSchema";
            builder.append( String.format( "%s.%s( ", className, methodName));
            formatArgument( builder, schema );
            builder.append( " ).materialise( " ).append( String.valueOf( descriptor.getId() ) ).append( " )" );
        }
        else if ( arg instanceof LabelSchemaDescriptor )
        {
            LabelSchemaDescriptor descriptor = (LabelSchemaDescriptor) arg;
            String className = SchemaDescriptor.class.getSimpleName();
            int labelId = descriptor.getLabelId();
            builder.append( format( "%s.forLabel( %d, %s )",
                    className, labelId, asString( descriptor.getPropertyIds() ) ) );
        }
        else if ( arg instanceof UniquenessConstraintDescriptor )
        {
            UniquenessConstraintDescriptor constraint = (UniquenessConstraintDescriptor) arg;
            String className = ConstraintDescriptorFactory.class.getSimpleName();
            int labelId = constraint.schema().getLabelId();
            builder.append( format( "%s.uniqueForLabel( %d, %s )",
                    className, labelId, asString( constraint.schema().getPropertyIds() ) ) );
        }
        else if ( arg instanceof NodeExistenceConstraintDescriptor )
        {
            NodeExistenceConstraintDescriptor constraint = (NodeExistenceConstraintDescriptor) arg;
            String className = ConstraintDescriptorFactory.class.getSimpleName();
            int labelId = constraint.schema().getLabelId();
            builder.append( format( "%s.existsForLabel( %d, %s )",
                    className, labelId, asString( constraint.schema().getPropertyIds() ) ) );
        }
        else if ( arg instanceof RelExistenceConstraintDescriptor )
        {
            RelationTypeSchemaDescriptor descriptor = ((RelExistenceConstraintDescriptor) arg).schema();
            String className = ConstraintDescriptorFactory.class.getSimpleName();
            int relTypeId = descriptor.getRelTypeId();
            builder.append( format( "%s.existsForReltype( %d, %s )",
                    className, relTypeId, asString( descriptor.getPropertyIds() ) ) );
        }
        else if ( arg instanceof NodeKeyConstraintDescriptor )
        {
            NodeKeyConstraintDescriptor constraint = (NodeKeyConstraintDescriptor) arg;
            String className = ConstraintDescriptorFactory.class.getSimpleName();
            int labelId = constraint.schema().getLabelId();
            builder.append( format( "%s.nodeKeyForLabel( %d, %s )",
                    className, labelId, asString( constraint.schema().getPropertyIds() ) ) );
        }
        else
        {
            throw new IllegalArgumentException(
                    format( "Can't handle argument of type: %s with value: %s", arg.getClass(), arg ) );
        }
    }

    private String asString( int[] propertyIds )
    {
        List<String> strings = Arrays.stream( propertyIds ).mapToObj( i -> "" + i ).collect( Collectors.toList() );
        return String.join( ", ", strings );
    }
}
