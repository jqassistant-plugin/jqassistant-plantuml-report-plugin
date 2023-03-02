package org.jqassistant.plugin.asciidocreport.plantuml.clazz;

import java.util.*;

import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.graph.SubGraphFactory;
import com.buschmais.jqassistant.core.report.api.graph.model.Identifiable;
import com.buschmais.jqassistant.core.report.api.graph.model.Node;
import com.buschmais.jqassistant.core.report.api.graph.model.Relationship;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;
import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.AccessModifierDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.MemberDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.PackageMemberDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.TypeDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts {@link Result}s into {@link ClassDiagramResult}s.
 */
class ClassDiagramResultConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassDiagramResultConverter.class);

    private final SubGraphFactory subGraphFactory;

    ClassDiagramResultConverter(SubGraphFactory subGraphFactory) {
        this.subGraphFactory = subGraphFactory;
    }

    public ClassDiagramResult convert(Result<? extends ExecutableRule> result) throws ReportException {
        Map<PackageMemberDescriptor, Node> packageMembers = new LinkedHashMap<>();
        Map<MemberDescriptor, Node> typeMembers = new LinkedHashMap<>();
        Set<Relationship> relations = new LinkedHashSet<>();
        List<Map<String, Object>> rows = result.getRows();
        for (Map<String, Object> row : rows) {
            for (Object value : row.values()) {
                convertValue(value, packageMembers, typeMembers, relations);
            }
        }
        Map<PackageMemberDescriptor, Set<PackageMemberDescriptor>> children = getPackageMemberTree(packageMembers);
        Map<TypeDescriptor, Set<MemberDescriptor>> membersPerType = aggregateMembersPerType(packageMembers.keySet(), typeMembers.keySet());
        return ClassDiagramResult.builder().packageMembers(packageMembers).packageMemberTree(children).membersPerType(membersPerType).relations(relations)
                .build();
    }

    private void convertValue(Object value, Map<PackageMemberDescriptor, Node> packageMembers, Map<MemberDescriptor, Node> typeMembers,
            Set<Relationship> relations) throws ReportException {
        if (value instanceof Iterable<?>) {
            for (Object element : (Iterable<?>) value) {
                convertValue(element, packageMembers, typeMembers, relations);
            }
        } else {
            Identifiable identifiable = this.subGraphFactory.toIdentifiable(value);
            if (identifiable instanceof Node) {
                Node node = (Node) identifiable;
                if (value instanceof PackageMemberDescriptor) {
                    addMember(value, node, packageMembers);
                } else if (value instanceof MemberDescriptor) {
                    addMember(value, node, typeMembers);
                }
            } else if (identifiable instanceof Relationship) {
                Relationship relationship = (Relationship) identifiable;
                relations.add(relationship);
            }
        }
    }

    private Map<PackageMemberDescriptor, Set<PackageMemberDescriptor>> getPackageMemberTree(Map<PackageMemberDescriptor, Node> packageMembers) {
        Map<PackageMemberDescriptor, Set<PackageMemberDescriptor>> children = new LinkedHashMap<>();
        for (Map.Entry<PackageMemberDescriptor, Node> entry : packageMembers.entrySet()) {
            PackageMemberDescriptor packageMember = entry.getKey();
            PackageMemberDescriptor enclosingParent = getEnclosingParent(packageMember, packageMembers.keySet());
            children.computeIfAbsent(enclosingParent, key -> new LinkedHashSet<>()).add(packageMember);
        }
        return children;
    }

    private PackageMemberDescriptor getEnclosingParent(PackageMemberDescriptor packageMember, Set<PackageMemberDescriptor> packageMembers) {
        if (packageMember instanceof FileDescriptor) {
            FileDescriptor current = (FileDescriptor) packageMember;
            do {
                Optional<FileDescriptor> parent = current.getParents().stream().filter(p -> p instanceof PackageMemberDescriptor).findFirst();
                current = parent.isPresent() ? parent.get() : null;
            } while (current != null && !packageMembers.contains(current));
            return (PackageMemberDescriptor) current;
        }
        return null;
    }

    private <D extends Descriptor> void addMember(Object member, Node node, Map<D, Node> memberNodes) {
        if (!isSynthetic(member)) {
            memberNodes.put((D) member, node);
        }
    }

    private boolean isSynthetic(Object value) {
        if (value instanceof AccessModifierDescriptor) {
            AccessModifierDescriptor accessModifier = (AccessModifierDescriptor) value;
            return (accessModifier.isSynthetic() != null && accessModifier.isSynthetic());
        }
        return false;
    }

    private Map<TypeDescriptor, Set<MemberDescriptor>> aggregateMembersPerType(Set<PackageMemberDescriptor> packageMembers, Set<MemberDescriptor> members) {
        Map<TypeDescriptor, Set<MemberDescriptor>> membersPerType = new LinkedHashMap<>();
        for (MemberDescriptor member : members) {
            TypeDescriptor declaringType = member.getDeclaringType();
            if (packageMembers.contains(declaringType)) {
                membersPerType.computeIfAbsent(declaringType, key -> new LinkedHashSet<>()).add(member);
            } else {
                LOGGER.debug("Result contains '{}' but not the declaring type '{}', skipping.", member.getSignature(), declaringType.getFullQualifiedName());
            }
        }
        return membersPerType;
    }
}
