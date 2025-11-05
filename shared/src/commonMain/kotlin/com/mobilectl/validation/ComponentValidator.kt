package com.mobilectl.validation

import com.mobilectl.config.Config
import com.mobilectl.model.ValidationError


interface ComponentValidator {
    fun validate(config: Config): List<ValidationError>
}


