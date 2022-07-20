package io.smetweb.json

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Convert
import jakarta.persistence.Converter
import jakarta.persistence.Entity
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.node.BaseJsonNode

/**
 * [TreeNodeJpaConverter] is a JPA [AttributeConverter]
 * that converts an [@Entity][Entity] object's Jackson [TreeNode] attributes
 * from/to their respective JSON<-formatted text column values (see https://json.org).
 *
 * Usage: [`@Convert`][Convert]`(`[`converter`][Convert.converter]`
 * =`[`TreeNodeJpaConverter.class`][TreeNodeJpaConverter]`)`
 */
// TreeNode interface does not extend Serializable, as Hibernate would like, see https://stackoverflow.com/a/37842443
@Converter(autoApply = true)
class TreeNodeJpaConverter : AttributeConverter<BaseJsonNode, String> {

	override fun convertToDatabaseColumn(attribute: BaseJsonNode?) = attribute
		?.let(OBJECT_MAPPER::writeValueAsString)

	override fun convertToEntityAttribute(dbData: String?) = dbData
		?.let(OBJECT_MAPPER::readTree) as BaseJsonNode
}