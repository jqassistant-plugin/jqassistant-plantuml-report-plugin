package org.jqassistant.plugin.asciidocreport.plantuml.clazz;

import java.util.Map;
import java.util.Set;

import com.buschmais.jqassistant.core.report.api.graph.model.Node;
import com.buschmais.jqassistant.core.report.api.graph.model.Relationship;
import com.buschmais.jqassistant.plugin.java.api.model.MemberDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.PackageMemberDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.TypeDescriptor;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class ClassDiagramResult {

    private Map<PackageMemberDescriptor, Node> packageMembers;

    private Map<PackageMemberDescriptor, Set<PackageMemberDescriptor>> packageMemberTree;

    private Map<TypeDescriptor, Set<MemberDescriptor>> membersPerType;

    private Set<Relationship> relations;

}
