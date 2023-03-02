package org.jqassistant.plugin.asciidocreport.plantuml.clazz;

import java.util.*;

import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.graph.SubGraphFactory;
import com.buschmais.jqassistant.core.report.api.graph.model.Node;
import com.buschmais.jqassistant.core.report.api.graph.model.Relationship;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;
import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.jqassistant.plugin.java.api.model.*;
import com.buschmais.xo.api.CompositeObject;

import org.jqassistant.plugin.asciidocreport.SubGraphTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.jqassistant.plugin.asciidocreport.SubGraphTestHelper.getRelationship;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ClassDiagramResultConverterTest {

    private static final String PACKAGE = "package";
    private static final String TYPE = "type";
    private static final String MEMBER = "member";
    private static final String RELATION = "relation";

    @Mock
    private SubGraphFactory subGraphFactory;

    private ClassDiagramResultConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ClassDiagramResultConverter(subGraphFactory);
    }

    /**
     * Verify conversion of
     * <ul>
     * <li>A root package containing a child package containing a type and its super
     * type</li>
     * <li>The type declares a field</li>
     * <li>The child package is not contained in the result, i.e. the root package
     * is treated as parent for the types</li>
     * </ul>
     *
     * @throws ReportException
     */
    @Test
    void convert() throws ReportException {
        // given
        doReturn(null).when(subGraphFactory).toIdentifiable(null);
        PackageDescriptor rootPackage = mock(PackageDescriptor.class);
        Node rootPackageNode = SubGraphTestHelper.getNode(1, "root");
        doReturn(rootPackageNode).when(subGraphFactory).toIdentifiable(rootPackage);

        PackageDescriptor childPackage = mock(PackageDescriptor.class);
        doReturn(new HashSet<>(asList(rootPackage))).when(childPackage).getParents();

        ClassTypeDescriptor type = mock(ClassTypeDescriptor.class);
        Node typeNode = SubGraphTestHelper.getNode(3, "root.child.Type");
        doReturn(new HashSet<>(asList(childPackage))).when(type).getParents();
        doReturn(typeNode).when(subGraphFactory).toIdentifiable(type);

        ClassTypeDescriptor superType = mock(ClassTypeDescriptor.class);
        Node superTypeNode = SubGraphTestHelper.getNode(4, "root.child.SuperType");
        doReturn(new HashSet<>(asList(childPackage))).when(superType).getParents();
        doReturn(superTypeNode).when(subGraphFactory).toIdentifiable(superType);

        FieldDescriptor field = mock(FieldDescriptor.class);
        doReturn(type).when(field).getDeclaringType();
        Node fieldNode = SubGraphTestHelper.getNode(5, "field");
        doReturn(fieldNode).when(subGraphFactory).toIdentifiable(field);

        CompositeObject relation = mock(CompositeObject.class);
        Relationship extendsRelation = SubGraphTestHelper.getRelationship(1, typeNode, "EXTENDS", superTypeNode);
        doReturn(extendsRelation).when(subGraphFactory).toIdentifiable(relation);

        List<Map<String, Object>> rows = new ArrayList<>();
        addRow(rootPackage, type, field, relation, rows);
        addRow(rootPackage, superType, null, null, rows);
        Result<ExecutableRule> result = Result.builder().columnNames(asList(PACKAGE, TYPE, MEMBER, RELATION)).rows(rows).build();

        // when
        ClassDiagramResult classDiagramResult = converter.convert(result);

        // then
        Map<PackageMemberDescriptor, Node> packageMembers = classDiagramResult.getPackageMembers();
        assertThat(packageMembers.size()).isEqualTo(3);
        assertThat(packageMembers.get(rootPackage)).isSameAs(rootPackageNode);
        assertThat(packageMembers.get(type)).isSameAs(typeNode);
        assertThat(packageMembers.get(superType)).isSameAs(superTypeNode);

        Map<PackageMemberDescriptor, Set<PackageMemberDescriptor>> packageMemberTree = classDiagramResult.getPackageMemberTree();
        assertThat(packageMemberTree.size()).isEqualTo(2);
        assertThat(packageMemberTree.get(null)).containsExactly(rootPackage);
        assertThat(packageMemberTree.get(rootPackage)).containsExactly(type, superType);

        Map<TypeDescriptor, Set<MemberDescriptor>> membersPerType = classDiagramResult.getMembersPerType();
        assertThat(membersPerType.size()).isEqualTo(1);
        assertThat(membersPerType.get(type)).containsExactly(field);

        Set<Relationship> relations = classDiagramResult.getRelations();
        assertThat(relations.size()).isEqualTo(1);
        assertThat(relations).containsExactly(extendsRelation);
    }

    @Test
    void convertIterable() throws ReportException {
        // given
        PackageDescriptor rootPackage = mock(PackageDescriptor.class);
        Node rootPackageNode = SubGraphTestHelper.getNode(1, "root");
        doReturn(rootPackageNode).when(subGraphFactory).toIdentifiable(rootPackage);

        PackageDescriptor subPackage = mock(PackageDescriptor.class);
        Node subPackageNode = SubGraphTestHelper.getNode(2, "sub");
        doReturn(subPackageNode).when(subGraphFactory).toIdentifiable(subPackage);

        Descriptor contains = mock(Descriptor.class);
        Relationship containsRelationship = SubGraphTestHelper.getRelationship(1, rootPackageNode, "CONTAINS", subPackageNode);
        doReturn(containsRelationship).when(subGraphFactory).toIdentifiable(contains);

        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put(PACKAGE, asList(rootPackage, subPackage));
        row.put(RELATION, asList(contains));
        rows.add(row);
        Result<ExecutableRule> result = Result.builder().columnNames(asList(PACKAGE)).rows(rows).build();

        // when
        ClassDiagramResult classDiagramResult = converter.convert(result);

        // then
        assertThat(classDiagramResult.getPackageMembers()).hasSize(2);
        assertThat(classDiagramResult.getRelations()).hasSize(1);
    }

    private void addRow(PackageDescriptor packageDescriptor, ClassTypeDescriptor typeDescriptor, MemberDescriptor memberDescriptor,
            CompositeObject relationship, List<Map<String, Object>> rows) {
        Map<String, Object> row = new HashMap<>();
        row.put(PACKAGE, packageDescriptor);
        row.put(TYPE, typeDescriptor);
        row.put(MEMBER, memberDescriptor);
        row.put(RELATION, relationship);
        rows.add(row);
    }
}
