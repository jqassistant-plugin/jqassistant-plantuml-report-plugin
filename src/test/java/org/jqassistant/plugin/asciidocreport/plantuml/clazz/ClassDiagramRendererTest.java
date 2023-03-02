package org.jqassistant.plugin.asciidocreport.plantuml.clazz;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.graph.model.Node;
import com.buschmais.jqassistant.core.report.api.graph.model.Relationship;
import com.buschmais.jqassistant.core.report.api.model.Result;
import org.jqassistant.plugin.asciidocreport.plantuml.RenderMode;
import com.buschmais.jqassistant.plugin.java.api.model.*;

import org.jqassistant.plugin.asciidocreport.SubGraphTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.jqassistant.plugin.asciidocreport.SubGraphTestHelper.getRelationship;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ClassDiagramRendererTest {

    @Mock
    private ClassDiagramResultConverter resultConverter;

    @Mock
    private Result<?> result;

    private ClassDiagramRenderer classDiagramRenderer;

    Map<PackageMemberDescriptor, Node> packageMembers;

    Map<PackageMemberDescriptor, Set<PackageMemberDescriptor>> packageMemberTree;

    Map<TypeDescriptor, Set<MemberDescriptor>> membersPerType;

    Set<Relationship> relations;

    @BeforeEach
    void setUp() throws ReportException {
        classDiagramRenderer = new ClassDiagramRenderer(resultConverter, RenderMode.GRAPHVIZ);
        packageMembers = new HashMap<>();
        packageMemberTree = new HashMap<>();
        membersPerType = new HashMap<>();
        relations = new HashSet<>();
        ClassDiagramResult classDiagramResult = ClassDiagramResult.builder().packageMembers(packageMembers).packageMemberTree(packageMemberTree)
                .membersPerType(membersPerType).relations(relations).build();
        doReturn(classDiagramResult).when(resultConverter).convert(result);
    }

    @Test
    void packageMembers() throws ReportException {
        PackageDescriptor packageDescriptor = addPackageMember(PackageDescriptor.class, 1, "public", null, "foo.bar");
        ClassTypeDescriptor classType = addPackageMember(ClassTypeDescriptor.class, 2, "public", null, "foo.bar.ClassType");

        InterfaceTypeDescriptor interfaceType = addPackageMember(InterfaceTypeDescriptor.class, 3, "public", null, "foo.bar.InterfaceType");
        AnnotationTypeDescriptor annotationType = addPackageMember(AnnotationTypeDescriptor.class, 4, "public", null, "foo.bar.AnnotationType");
        EnumTypeDescriptor enumType = addPackageMember(EnumTypeDescriptor.class, 5, "public", null, "foo.bar.EnumType");

        packageMemberTree.computeIfAbsent(null, key -> new HashSet<>()).add(packageDescriptor);
        packageMemberTree.computeIfAbsent(packageDescriptor, key -> new HashSet<>()).addAll(asList(classType, interfaceType, annotationType, enumType));

        String diagram = classDiagramRenderer.renderDiagram(result);

        assertThat(diagram).contains("package n1 as \"foo.bar\"{");
        assertThat(diagram).contains("  +class n2 as \"foo.bar.ClassType\"{");
        assertThat(diagram).contains("  +interface n3 as \"foo.bar.InterfaceType\"{");
        assertThat(diagram).contains("  +annotation n4 as \"foo.bar.AnnotationType\"");
        assertThat(diagram).contains("  +enum n5 as \"foo.bar.EnumType\"{");
    }

    @Test
    void classModifiers() throws ReportException {
        Set<PackageMemberDescriptor> rootMembers = packageMemberTree.computeIfAbsent(null, key -> new HashSet<>());
        rootMembers.add(addPackageMember(ClassTypeDescriptor.class, 1, null, true, "foo.bar.AbstractClassType"));
        rootMembers.add(addPackageMember(ClassTypeDescriptor.class, 2, "private", false, "foo.bar.PrivateClassType"));
        rootMembers.add(addPackageMember(ClassTypeDescriptor.class, 3, "public", false, "foo.bar.PublicClassType"));
        rootMembers.add(addPackageMember(ClassTypeDescriptor.class, 4, "protected", false, "foo.bar.ProtectedClassType"));
        rootMembers.add(addPackageMember(ClassTypeDescriptor.class, 5, "default", false, "foo.bar.DefaultClassType"));
        rootMembers.add(addPackageMember(ClassTypeDescriptor.class, 6, null, false, "foo.bar.ClassType"));

        String diagram = classDiagramRenderer.renderDiagram(result);

        assertThat(diagram).contains("abstract class n1 as \"foo.bar.AbstractClassType\"{");
        assertThat(diagram).contains("-class n2 as \"foo.bar.PrivateClassType\"{");
        assertThat(diagram).contains("+class n3 as \"foo.bar.PublicClassType\"{");
        assertThat(diagram).contains("#class n4 as \"foo.bar.ProtectedClassType\"{");
        assertThat(diagram).contains("~class n5 as \"foo.bar.DefaultClassType\"{");
        assertThat(diagram).contains("class n6 as \"foo.bar.ClassType\"{");
    }

    @Test
    void fields() throws ReportException {
        verifyMembers(FieldDescriptor.class, "Field");
    }

    @Test
    void methods() throws ReportException {
        verifyMembers(MethodDescriptor.class, "Method");
    }

    @Test
    void abstractMethod() throws ReportException {
        ClassTypeDescriptor typeDescriptor = getClassType();
        Set<MemberDescriptor> members = membersPerType.computeIfAbsent(typeDescriptor, key -> new HashSet<>());
        members.add(addTypeMember(MethodDescriptor.class, "public", null, true, "String abstractMethod"));

        String diagram = classDiagramRenderer.renderDiagram(result);

        assertThat(diagram).contains("  {abstract} +String abstractMethod");
    }

    @Test
    void fieldAssociation() throws ReportException {
        Set<PackageMemberDescriptor> rootMembers = packageMemberTree.computeIfAbsent(null, key -> new HashSet<>());
        ClassTypeDescriptor classType = addPackageMember(ClassTypeDescriptor.class, 1, "public", null, "foo.bar.ClassType");
        ClassTypeDescriptor fieldType = addPackageMember(ClassTypeDescriptor.class, 2, "public", null, "foo.bar.FieldType");
        rootMembers.add(classType);
        rootMembers.add(fieldType);
        membersPerType.put(fieldType, emptySet());

        FieldDescriptor field = mock(FieldDescriptor.class);
        doReturn(classType).when(field).getDeclaringType();
        doReturn(fieldType).when(field).getType();
        membersPerType.computeIfAbsent(classType, key -> new HashSet<>()).add(field);
        doReturn("public").when(field).getVisibility();
        doReturn("associatedField").when(field).getName();

        String diagram = classDiagramRenderer.renderDiagram(result);

        assertThat(diagram).contains("n1 -> n2");
    }

    @Test
    void relations() throws ReportException {
        Set<PackageMemberDescriptor> rootMembers = packageMemberTree.computeIfAbsent(null, key -> new HashSet<>());
        ClassTypeDescriptor classType = addPackageMember(ClassTypeDescriptor.class, 1, "public", null, "foo.bar.ClassType");
        ClassTypeDescriptor superClassType = addPackageMember(ClassTypeDescriptor.class, 2, "public", null, "foo.bar.SuperClassType");
        InterfaceTypeDescriptor interfaceType = addPackageMember(InterfaceTypeDescriptor.class, 3, "public", null, "foo.bar.InterfaceType");
        rootMembers.add(classType);
        rootMembers.add(superClassType);
        rootMembers.add(interfaceType);

        relations.add(SubGraphTestHelper.getRelationship(1, packageMembers.get(classType), "EXTENDS", packageMembers.get(superClassType)));
        relations.add(SubGraphTestHelper.getRelationship(2, packageMembers.get(classType), "IMPLEMENTS", packageMembers.get(interfaceType)));
        relations.add(SubGraphTestHelper.getRelationship(3, packageMembers.get(classType), "DEPENDS_ON", packageMembers.get(superClassType)));
        relations.add(SubGraphTestHelper.getRelationship(4, packageMembers.get(classType), "DEPENDS_ON", packageMembers.get(interfaceType)));

        String diagram = classDiagramRenderer.renderDiagram(result);

        assertThat(diagram).contains("n1--|>n2");
        assertThat(diagram).contains("n1..|>n3");
        assertThat(diagram).contains("n1-->n2");
        assertThat(diagram).contains("n1-->n3");
    }

    private void verifyMembers(Class<? extends MemberDescriptor> memberType, String signatureSuffix) throws ReportException {
        ClassTypeDescriptor typeDescriptor = getClassType();
        Set<MemberDescriptor> members = membersPerType.computeIfAbsent(typeDescriptor, key -> new HashSet<>());
        members.add(addTypeMember(memberType, "private", null, null, "String private" + signatureSuffix));
        members.add(addTypeMember(memberType, "public", null, null, "String public" + signatureSuffix));
        members.add(addTypeMember(memberType, "protected", null, null, "String protected" + signatureSuffix));
        members.add(addTypeMember(memberType, "default", null, null, "String default" + signatureSuffix));
        members.add(addTypeMember(memberType, null, null, null, "String required" + signatureSuffix));
        members.add(addTypeMember(memberType, null, true, null, "String static" + signatureSuffix));

        String diagram = classDiagramRenderer.renderDiagram(result);

        assertThat(diagram).contains("  -String private" + signatureSuffix);
        assertThat(diagram).contains("  +String public" + signatureSuffix);
        assertThat(diagram).contains("  #String protected" + signatureSuffix);
        assertThat(diagram).contains("  ~String default" + signatureSuffix);
        assertThat(diagram).contains("  String required" + signatureSuffix);
        assertThat(diagram).contains("  {static} String static" + signatureSuffix);
    }

    private ClassTypeDescriptor getClassType() {
        Set<PackageMemberDescriptor> rootMembers = packageMemberTree.computeIfAbsent(null, key -> new HashSet<>());
        ClassTypeDescriptor typeDescriptor = addPackageMember(ClassTypeDescriptor.class, 1, null, null, "foo.bar.ClassType");
        rootMembers.add(typeDescriptor);
        return typeDescriptor;
    }

    private <D extends PackageMemberDescriptor> D addPackageMember(Class<D> type, long id, String visibility, Boolean isAbstract, String fqn) {
        D packageMember = mock(type);
        doReturn(fqn).when(packageMember).getFullQualifiedName();
        if (packageMember instanceof AccessModifierDescriptor) {
            doReturn(visibility).when((AccessModifierDescriptor) packageMember).getVisibility();
        }
        if (packageMember instanceof AbstractDescriptor) {
            doReturn(isAbstract).when(((AbstractDescriptor) packageMember)).isAbstract();
        }
        packageMembers.put(packageMember, SubGraphTestHelper.getNode(id, fqn));
        return packageMember;
    }

    private <D extends MemberDescriptor> D addTypeMember(Class<D> type, String visibility, Boolean isStatic, Boolean isAbstract, String signature) {
        D member = mock(type);
        doReturn(visibility).when(member).getVisibility();
        doReturn(isStatic).when(member).isStatic();
        doReturn(signature).when(member).getSignature();
        if (member instanceof AbstractDescriptor) {
            doReturn(isAbstract).when(((AbstractDescriptor) member)).isAbstract();
        }
        return member;
    }
}
