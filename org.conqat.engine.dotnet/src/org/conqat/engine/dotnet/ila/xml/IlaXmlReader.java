/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright 2005-2011 The ConQAT Project                                   |
|                                                                          |
| Licensed under the Apache License, Version 2.0 (the "License");          |
| you may not use this file except in compliance with the License.         |
| You may obtain a copy of the License at                                  |
|                                                                          |
|    http://www.apache.org/licenses/LICENSE-2.0                            |
|                                                                          |
| Unless required by applicable law or agreed to in writing, software      |
| distributed under the License is distributed on an "AS IS" BASIS,        |
| WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. |
| See the License for the specific language governing permissions and      |
| limitations under the License.                                           |
+-------------------------------------------------------------------------*/
package org.conqat.engine.dotnet.ila.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.conqat.engine.commons.keys.IDependencyListKey;
import org.conqat.engine.commons.node.ListNode;
import org.conqat.engine.commons.node.NodeUtils;
import org.conqat.engine.commons.pattern.PatternList;
import org.conqat.engine.core.core.ConQATException;
import org.conqat.engine.dotnet.ila.ILDependenciesImporterProcessor;
import org.conqat.engine.dotnet.ila.ILImporterProcessorBase;
import org.conqat.engine.dotnet.ila.Member;
import org.conqat.engine.resource.text.ITextElement;
import org.conqat.lib.commons.collections.ToStringComparator;
import org.conqat.lib.commons.error.NeverThrownRuntimeException;
import org.conqat.lib.commons.string.StringUtils;
import org.conqat.lib.commons.xml.IXMLElementProcessor;

/**
 * Reads members from Intermediate Language Analyzer XML files into a
 * representation that can be used for architecture assessments.
 * <p>
 * The currently implemented approach is very simplistic: (Change it as soon as
 * need arises!). All classes encountered in the IL-XML are read and appended as
 * children to a single root. Dependencies are stored in a key called
 * {@value IDependencyListKey#DEPENDENCY_LIST_KEY}.
 * 
 * @author $Author: streitel $
 * @version $Revision: 46299 $
 * @ConQAT.Rating YELLOW Hash: A1A8F4C75068E71FE38192DEAE32F3AD
 */
public class IlaXmlReader extends IlaXmlReaderBase<NeverThrownRuntimeException> {

	/** Cache of type nodes referenced by their node ID. */
	private final HashMap<String, ListNode> typeCache = new HashMap<String, ListNode>();

	/** Flag that determines whether to ignore types generated by the compiler. */
	private final boolean ignoreSynthetic;

	/**
	 * Flag that determines whether method bodies are read from XML or not
	 * (impacts memory footprint)
	 */
	private final boolean includeBody;

	/**
	 * If not null, types whose base class matches one of these patters are
	 * ignored.
	 */
	private final PatternList ignoreBaseTypePatterns;

	/**
	 * Constructor.
	 * 
	 * @param element
	 *            XML that gets parsed.
	 * @param root
	 *            Root under which the nodes representing types found during
	 *            parse are attached
	 * @param ignorePatterns
	 *            List of patterns for members that are ignored during import.
	 * @param includePatterns
	 *            If not null, only members matching one of these patterns are
	 *            included.
	 * @param ignoreBaseTypePatterns
	 *            If not null, only types whose base types are not matched are
	 *            processed.
	 * @param includeBody
	 *            Flag that determines whether the method body gets read from
	 *            XML.
	 */
	public IlaXmlReader(ITextElement element, ListNode root,
			PatternList ignorePatterns, PatternList includePatterns,
			PatternList ignoreBaseTypePatterns, boolean ignoreSynthetic,
			boolean includeBody, Set<String> includedDependencies,
			Set<String> excludedDependencies) throws ConQATException {
		super(element, root, ignorePatterns, includePatterns,
				includedDependencies, excludedDependencies);
		this.ignoreSynthetic = ignoreSynthetic;
		this.ignoreBaseTypePatterns = ignoreBaseTypePatterns;
		this.includeBody = includeBody;
	}

	/**
	 * Constructor that is used from test cases.
	 * 
	 * @param element
	 *            XML that gets parsed.
	 * @param root
	 *            Root under which the nodes representing types found during
	 *            parse are attached.
	 */
	/* package */IlaXmlReader(ITextElement element, ListNode root,
			Set<String> includedDependencies, Set<String> excludedDependencies)
			throws ConQATException {
		this(element, root, new PatternList(), null, null, true, false,
				includedDependencies, excludedDependencies);
	}

	/** {@inheritDoc} */
	@Override
	protected void doParse() {
		String assemblyName = getStringAttribute(EIlaXmlAttribute.Name);
		processChildElements(new XmlTypeElementReader(assemblyName));
	}

	/** Retrieve the number of IL statements from the current element */
	private int parseNumerOfIlStmts() {
		String numberIlStatementsString = getStringAttribute(EIlaXmlAttribute.NumILStmts);

		if (StringUtils.isEmpty(numberIlStatementsString)) {
			return 0;
		}

		return Integer.valueOf(numberIlStatementsString);
	}

	/** Processor for IlaXML nodes containing type information. */
	private abstract class IlaXMLElementProcessor implements
			IXMLElementProcessor<EIlaXmlElement, NeverThrownRuntimeException> {

		/**
		 * Returns the type name with IL-generated classes stripped from the
		 * type name.
		 */
		public String getTypeName() {

			String typeName = getStringAttribute(EIlaXmlAttribute.Type);

			// TODO (EJ) If I understand the code correctly, it does not remove
			// the class, but only the synthetic suffix. This is a small thing,
			// but it would be cool if you could clarify this in the comment
			// (and example)
			// (MP) The synthetic suffix /Function actually is a class. Stated
			// this in the comment.

			// Removes synthetic classes produced by yield statements. The
			// function is converted into an inner class
			//
			// Example:
			// namespace Namespace
			// {
			// class Class : Ifc
			// {
			// IEnumeration<int> getItem()
			// {
			// yield return 0;
			// }
			// IEnumeration<int> Ifc.getItem2()
			// {
			// yield return 0;
			// }
			// }
			//
			// interface Ifc {
			// IEnumeration<int> getItem2()
			// }
			// }
			//
			// Becomes for getItem:
			// Namespace.Class/d__1
			// for getItem2:
			// Namespace.Class/getItem2>d__2
			typeName = typeName.replaceAll("/(.*>)?d__[\\da-fA-F]+", "");
			typeName = typeName.replaceAll("`\\d+.*", "");

			return typeName;
		}
	}

	/** Processor for &lt;TypeElement&gt; nodes. */
	private class XmlTypeElementReader extends IlaXMLElementProcessor {

		/** Keeps track of the assembly we are currently processing */
		private final String assemblyName;

		/** Constructor */
		public XmlTypeElementReader(String assemblyName) {
			this.assemblyName = assemblyName;
		}

		/** {@inheritDoc} */
		@Override
		public void process() {
			if (skipType()) {
				return;
			}

			String typeName = getTypeName();
			ListNode currentElementNode = typeCache.get(typeName);
			if (currentElementNode == null) {
				currentElementNode = new ListNode(typeName);
				typeCache.put(typeName, currentElementNode);
				root.addChild(currentElementNode);

				currentElementNode.setValue(
						ILImporterProcessorBase.ASSEMBLY_NAME, assemblyName);

				// We cannot store this information twice, if the type is
				// encountered again (e.g. by mapping il-generated classes and
				// dependencies). Usually the first type found in the XML is the
				// hand-written one, so we store this information for the first
				// one which is encountered.
				parseDeclType(currentElementNode);
				parseToken(currentElementNode);
			}

			int ilStatementCount = parseNumerOfIlStmts()
					+ NodeUtils.getValue(currentElementNode,
							ILDependenciesImporterProcessor.IL_STATEMENT_COUNT,
							Integer.class, 0);

			currentElementNode.setValue(
					ILDependenciesImporterProcessor.IL_STATEMENT_COUNT,
					ilStatementCount);

			parseDependencies(currentElementNode, typeName);
			parseMembers(currentElementNode,
					getBooleanAttribute(EIlaXmlAttribute.Synthetic));
		}

		/** Read declaration type from XML into node. */
		private void parseDeclType(ListNode currentElementNode) {
			parseAndStoreAttribute(EIlaXmlAttribute.DeclType,
					ILDependenciesImporterProcessor.DECLTYPE,
					currentElementNode);
		}

		/** Parse metadata token from XML into node. */
		private void parseToken(ListNode currentElementNode) {
			parseAndStoreAttribute(EIlaXmlAttribute.Token,
					ILDependenciesImporterProcessor.TOKEN, currentElementNode);
		}

		/** Parse attribute value from XML and store in node. */
		private void parseAndStoreAttribute(EIlaXmlAttribute attributeName,
				String targetKey, ListNode node) {
			String value = getStringAttribute(attributeName);
			node.setValue(targetKey, value);
		}

		/** Read dependencies from XML into node. */
		private Collection<String> parseDependencies(
				ListNode currentElementNode, String typeName) {
			Collection<String> dependencies = new HashSet<String>();

			processDecendantElements(new DependsReader(dependencies, typeName,
					EIlaXmlElement.Extends));
			processDecendantElements(new DependsReader(dependencies, typeName,
					EIlaXmlElement.Implements));
			currentElementNode.setValue(IDependencyListKey.SUPER_TYPE_LIST_KEY,
					new HashSet<String>(dependencies));

			processDecendantElements(new DependsReader(dependencies, typeName,
					EIlaXmlElement.Depends));
			currentElementNode.setValue(IDependencyListKey.DEPENDENCY_LIST_KEY,
					dependencies);

			return dependencies;
		}

		/**
		 * Parse members from XMl into node
		 * 
		 * @param isSynthetic
		 *            Flag that determines whether member is synthesized by the
		 *            compiler (and thus does not appear in the source code)
		 */
		private void parseMembers(ListNode currentElementNode,
				boolean isSynthetic) {
			List<Member> members = new ArrayList<Member>();
			processDecendantElements(new MemberReader(members, isSynthetic));
			Collections.sort(members, ToStringComparator.INSTANCE);
			currentElementNode.setValue(
					ILDependenciesImporterProcessor.MEMBERS, members);
		}

		/** Determines whether a type should be skipped. */
		private boolean skipType() {
			boolean isSynthetic = getBooleanAttribute(EIlaXmlAttribute.Synthetic);
			if (isSynthetic && ignoreSynthetic) {
				return true;
			}

			if (ignoreBaseTypePatterns == null) {
				return false;
			}

			// determine base type
			BaseTypeReader reader = new BaseTypeReader();
			processChildElements(reader);
			String baseType = reader.getBaseType();

			// match base type against ignore list
			boolean ignore = ignoreBaseTypePatterns.matchesAny(baseType);
			if (ignore) {
				String typeName = getTypeName();
				System.err.println("Ingoring " + typeName + ": Base type: "
						+ baseType);
			}
			return ignore;
		}

		/** {@inheritDoc} */
		@Override
		public EIlaXmlElement getTargetElement() {
			return EIlaXmlElement.TypeElement;
		}

	}

	/** Determines the base type of a type element */
	private class BaseTypeReader extends IlaXMLElementProcessor {

		/** Field to store base type. */
		private String baseType;

		/** {@inheritDoc} */
		@Override
		public EIlaXmlElement getTargetElement() {
			return EIlaXmlElement.Extends;
		}

		/** {@inheritDoc} */
		@Override
		public void process() throws NeverThrownRuntimeException {
			baseType = getTypeName();
		}

		/** Returns base type. */
		public String getBaseType() {
			return baseType;
		}

	}

	/**
	 * Processor that processes dependencies. Target element is passed via the
	 * constructor, since it needs to operate on different target elements.
	 */
	private class DependsReader extends IlaXMLElementProcessor {

		/** Reference to list in which members are stored. */
		private final Collection<String> dependencies;

		/** Target element this processor works on. */
		private final EIlaXmlElement targetElement;

		/** Name of type we are currently working on. */
		private final String typeName;

		/** Constructor. */
		public DependsReader(Collection<String> dependencies, String typeName,
				EIlaXmlElement targetElement) {
			this.dependencies = dependencies;
			this.targetElement = targetElement;
			this.typeName = typeName;
		}

		/** {@inheritDoc} */
		@Override
		public EIlaXmlElement getTargetElement() {
			return targetElement;
		}

		/** {@inheritDoc} */
		@Override
		public void process() {
			String target = getTypeName();
			boolean included = processDependency(typeName, target);
			if (included) {
				dependencies.add(target);
			}
		}
	}

	/** Processor for &lt;Depends&gt; nodes. */
	private class MemberReader implements
			IXMLElementProcessor<EIlaXmlElement, NeverThrownRuntimeException> {

		/** Reference to list in which members are stored. */
		private final List<Member> members;

		/** Flag that determines whether members are synthetic */
		private final boolean isSynthetic;

		/**
		 * Constructor.
		 * 
		 * @param isSynthetic
		 */
		protected MemberReader(List<Member> members, boolean isSynthetic) {
			this.members = members;
			this.isSynthetic = isSynthetic;
		}

		/** {@inheritDoc} */
		@Override
		public EIlaXmlElement getTargetElement() {
			return EIlaXmlElement.Member;
		}

		/** {@inheritDoc} */
		@Override
		public void process() {
			String name = getStringAttribute(EIlaXmlAttribute.Name);
			String type = getStringAttribute(EIlaXmlAttribute.MemberType);
			String visibility = getStringAttribute(EIlaXmlAttribute.Visibility);
			boolean isAbstract = getBooleanAttribute(EIlaXmlAttribute.IsAbstract);
			String token = getStringAttribute(EIlaXmlAttribute.Token);
			String ilStatementSequence = null;
			if (includeBody) {
				ilStatementSequence = getStringAttribute(EIlaXmlAttribute.BodyZip);
			}

			Member member = new Member(name, type, visibility, isAbstract,
					token, parseNumerOfIlStmts(), ilStatementSequence,
					isSynthetic);
			members.add(member);
		}
	}
}