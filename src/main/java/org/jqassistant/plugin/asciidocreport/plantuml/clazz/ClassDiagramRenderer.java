package org.jqassistant.plugin.asciidocreport.plantuml.clazz;

import java.util.*;

import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.graph.model.Node;
import com.buschmais.jqassistant.core.report.api.graph.model.Relationship;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;
import org.jqassistant.plugin.asciidocreport.plantuml.AbstractDiagramRenderer;
import org.jqassistant.plugin.asciidocreport.plantuml.RenderMode;
import com.buschmais.jqassistant.plugin.java.api.model.*;

import static java.util.Collections.emptySet;

public class ClassDiagramRenderer extends AbstractDiagramRenderer {

    private static final String DEFAULT_RELATION_TYPE = "-->";

    private ClassDiagramResultConverter classDiagramResultConverter;

    private Map<String, String> relationTypes;

    public ClassDiagramRenderer(ClassDiagramResultConverter classDiagramResultConverter, RenderMode renderMode) {
        super(renderMode);
        this.classDiagramResultConverter = classDiagramResultConverter;
        this.relationTypes = new HashMap<>();
        relationTypes.put("EXTENDS", "--|>");
        relationTypes.put("IMPLEMENTS", "..|>");
    }

    @Override
    protected void render(Result<? extends ExecutableRule> result, StringBuilder builder) throws ReportException {
        ClassDiagramResult classDiagramResult = classDiagramResultConverter.convert(result);
        // Render starting root package members, i.e. those without parent.
        Set<PackageMemberDescriptor> rootMembers = classDiagramResult.getPackageMemberTree().getOrDefault(null, emptySet());
        Set<FieldDescriptor> fieldAssociations = renderPackageMembers(rootMembers, classDiagramResult, 0, builder);
        // Render collected field associations
        renderAssociations(classDiagramResult.getPackageMembers(), fieldAssociations, builder);
        // Render custom relations
        HashSet<Node> packageMemberNodes = new HashSet<>(classDiagramResult.getPackageMembers().values());
        renderRelations(packageMemberNodes, classDiagramResult, builder);
    }

    private Set<FieldDescriptor> renderPackageMembers(Set<PackageMemberDescriptor> packageMembers, ClassDiagramResult classDiagramResult, int level,
            StringBuilder builder) {
        Set<FieldDescriptor> fieldAssociations = new LinkedHashSet<>();
        for (PackageMemberDescriptor packageMember : packageMembers) {
            builder.append(indent(level));
            Node node = classDiagramResult.getPackageMembers().get(packageMember);
            if (packageMember instanceof PackageDescriptor) {
                builder.append("package").append(' ').append(getNodeId(node)).append(" as ").append('"').append(packageMember.getFullQualifiedName())
                        .append('"');
                builder.append("{\n");
                Set<PackageMemberDescriptor> children = classDiagramResult.getPackageMemberTree().get(packageMember);
                fieldAssociations.addAll(renderPackageMembers(children, classDiagramResult, level + 1, builder));
                builder.append("}");
            } else if (packageMember instanceof TypeDescriptor) {
                TypeDescriptor type = (TypeDescriptor) packageMember;
                if (type instanceof AccessModifierDescriptor) {
                    builder.append(getVisibility((AccessModifierDescriptor) type));
                }
                if (isAbstract(type) && !(type instanceof EnumTypeDescriptor)) {
                    builder.append("abstract").append(" ");
                }
                builder.append(getType(type)).append(" ").append(getNodeId(node)).append(" as ").append('"').append(packageMember.getFullQualifiedName())
                        .append('"');
                if (!(type instanceof AnnotationTypeDescriptor)) {
                    // members only supported for classes, interfaces and enums (not: annotations)
                    builder.append("{\n");
                    Set<MemberDescriptor> typeMembers = classDiagramResult.getMembersPerType().getOrDefault(type, emptySet());
                    fieldAssociations.addAll(renderTypeMembers(typeMembers, classDiagramResult, level + 1, builder));
                    builder.append(indent(level)).append('}');
                }
            }
            builder.append("\n");
        }
        return fieldAssociations;
    }

    private Set<FieldDescriptor> renderTypeMembers(Set<MemberDescriptor> typeMembers, ClassDiagramResult classDiagramResult, int level, StringBuilder builder) {
        Set<FieldDescriptor> fieldAssociations = new LinkedHashSet<>();
        for (MemberDescriptor member : typeMembers) {
            if (member instanceof FieldDescriptor) {
                FieldDescriptor field = (FieldDescriptor) member;
                if (!isStatic(field) && classDiagramResult.getPackageMembers().containsKey(field.getType())) {
                    fieldAssociations.add(field);
                } else {
                    renderMemberSignature(member, level, builder);
                }
            } else if (member instanceof MethodDescriptor) {
                renderMemberSignature(member, level, builder);
            }
        }
        return fieldAssociations;
    }

    private void renderAssociations(Map<PackageMemberDescriptor, Node> packageMembers, Set<FieldDescriptor> fieldAssociations, StringBuilder builder) {
        for (FieldDescriptor fieldAssociation : fieldAssociations) {
            TypeDescriptor declaringType = fieldAssociation.getDeclaringType();
            TypeDescriptor fieldType = fieldAssociation.getType();
            builder.append(getNodeId(packageMembers.get(declaringType)));
            builder.append(" -> ");
            builder.append(getNodeId(packageMembers.get(fieldType)));
            builder.append(" : ").append(getVisibility(fieldAssociation)).append(fieldAssociation.getName());
            builder.append("\n");
        }
    }

    private void renderRelations(Set<Node> packageMemberNodes, ClassDiagramResult classDiagramResult, StringBuilder builder) {
        for (Relationship relationship : classDiagramResult.getRelations()) {
            String arrowType = relationTypes.getOrDefault(relationship.getType(), DEFAULT_RELATION_TYPE);
            Node startNode = relationship.getStartNode();
            Node endNode = relationship.getEndNode();
            if (packageMemberNodes.contains(startNode) && packageMemberNodes.contains(endNode)) {
                builder.append(getNodeId(startNode)).append(arrowType).append(getNodeId(endNode)).append("\n");
            }
        }
    }

    private String getType(TypeDescriptor type) {
        if (type instanceof InterfaceTypeDescriptor) {
            return "interface";
        } else if (type instanceof EnumTypeDescriptor) {
            return "enum";
        } else if (type instanceof AnnotationTypeDescriptor) {
            return "annotation";
        } else {
            return "class";
        }
    }

    private void renderMemberSignature(MemberDescriptor member, int level, StringBuilder builder) {
        builder.append(indent(level));
        if (isStatic(member)) {
            builder.append("{static}").append(" ");
        }
        if (isAbstract(member)) {
            builder.append("{abstract}").append(" ");
        }
        builder.append(getVisibility(member));
        builder.append(member.getSignature()).append("\n");
    }

    private boolean isStatic(MemberDescriptor member) {
        return member.isStatic() != null && member.isStatic();
    }

    private boolean isAbstract(JavaByteCodeDescriptor descriptor) {
        if (descriptor instanceof AbstractDescriptor) {
            AbstractDescriptor abstractDescriptor = (AbstractDescriptor) descriptor;
            return abstractDescriptor.isAbstract() != null ? abstractDescriptor.isAbstract() : false;
        }
        return false;
    }

    private String getVisibility(AccessModifierDescriptor accessModifierDescriptor) {
        if (accessModifierDescriptor.getVisibility() != null) {
            switch (accessModifierDescriptor.getVisibility()) {
            case "public":
                return "+";
            case "private":
                return "-";
            case "protected":
                return "#";
            case "default":
                return "~";
            default:
            }
        }
        return "";
    }
}
