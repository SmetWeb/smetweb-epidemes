package io.smetweb.json

import javax.persistence.AttributeConverter
import javax.persistence.Convert
import javax.persistence.Converter
import javax.persistence.Entity
import com.fasterxml.jackson.core.TreeNode

/**
 * [TreeNodeJpaConverter] is a JPA [AttributeConverter]
 * that converts an [@Entity][Entity] object's Jackson [TreeNode] attributes
 * from/to their respective JSON<-formatted text column values (see https://json.org).
 *
 * Usage: [`@Convert`][Convert]`(`[`converter`][Convert.converter]`
 * =`[`TreeNodeJpaConverter.class`][TreeNodeJpaConverter]`)`
 */
@Converter(autoApply = true)
class TreeNodeJpaConverter : AttributeConverter<TreeNode, String> {

	override fun convertToDatabaseColumn(attribute: TreeNode?) = attribute
			?.let(OBJECT_MAPPER::writeValueAsString)

	override fun convertToEntityAttribute(dbData: String?) = dbData
			?.let(OBJECT_MAPPER::readTree)
}