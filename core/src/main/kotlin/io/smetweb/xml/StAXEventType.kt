package io.smetweb.xml

import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader
import javax.xml.stream.events.*

enum class StAXEventType(
	val index: Int,
	val eventType: Class<out XMLEvent>,
) {

	/** Indicates an event is a start element */
	START_ELEMENT(XMLStreamConstants.START_ELEMENT, StartElement::class.java),

	/** Indicates an event is an end element */
	END_ELEMENT(XMLStreamConstants.END_ELEMENT, EndElement::class.java),

	/** Indicates an event is a processing instruction */
	PROCESSING_INSTRUCTION(XMLStreamConstants.PROCESSING_INSTRUCTION, ProcessingInstruction::class.java),

	/** Indicates an event is characters */
	CHARACTERS(XMLStreamConstants.CHARACTERS, Characters::class.java),

	/** Indicates an event is a comment */
	COMMENT(XMLStreamConstants.COMMENT, Comment::class.java),

	/**
	 * The characters are white space (see XML, 2.10 "White Space Handling").
	 * Events are only reported as SPACE if they are ignorable white space.
	 * Otherwise, they are reported as CHARACTERS.
	 */
	SPACE(XMLStreamConstants.SPACE, Characters::class.java),

	/** Indicates an event is a start document */
	START_DOCUMENT(XMLStreamConstants.START_DOCUMENT, StartDocument::class.java),

	/** Indicates an event is an end document */
	END_DOCUMENT(XMLStreamConstants.END_DOCUMENT, EndDocument::class.java),

	/** Indicates an event is an entity reference */
	ENTITY_REFERENCE(XMLStreamConstants.ENTITY_REFERENCE, EntityReference::class.java),

	/** Indicates an event is an attribute */
	ATTRIBUTE(XMLStreamConstants.ATTRIBUTE, Attribute::class.java),

	/** Indicates an event is a DTD */
	DTD(XMLStreamConstants.DTD, javax.xml.stream.events.DTD::class.java),

	/** Indicates an event is a CDATA section */
	CDATA(XMLStreamConstants.CDATA, Characters::class.java),

	/** Indicates the event is a namespace declaration */
	NAMESPACE(XMLStreamConstants.NAMESPACE, Namespace::class.java),

	/** Indicates a Notation */
	NOTATION_DECLARATION(XMLStreamConstants.NOTATION_DECLARATION, NotationDeclaration::class.java),

	/** Indicates an Entity Declaration */
	ENTITY_DECLARATION(XMLStreamConstants.ENTITY_DECLARATION, NotationDeclaration::class.java);

	companion object {

		/**
		 * @param xmlReader
		 * @return a [StAXEventType]
		 */
		fun valueOf(xmlReader: XMLStreamReader): StAXEventType =
			valueOf(xmlReader.eventType)

		/**
		 * @param eventType
		 * @return a [StAXEventType]
		 */
		fun valueOf(eventType: Int): StAXEventType {
			require(eventType <= values().size)
			return values()[eventType - 1]
		}
	}
}