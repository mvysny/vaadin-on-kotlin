package com.vaadin.starter.beveragebuddy.ui.converters

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.vaadin.flow.model.ModelConverter

/**
 * Converts between DateTime-objects and their String-representations
 *
 */

class LocalDateToStringConverter : ModelConverter<LocalDate, String> {

    override fun toModel(presentationValue: String): LocalDate {
        return LocalDate.parse(presentationValue, DATE_FORMAT)
    }

    override fun toPresentation(modelValue: LocalDate?): String? {
        return modelValue?.format(DATE_FORMAT)
    }

    companion object {

        private val DATE_FORMAT = DateTimeFormatter
                .ofPattern("MM/dd/yyyy")
    }

}
