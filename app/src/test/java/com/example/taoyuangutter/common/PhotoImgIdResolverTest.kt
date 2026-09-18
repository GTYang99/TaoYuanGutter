package com.example.taoyuangutter.common

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoImgIdResolverTest {
    @Test
    fun responseIdTakesPrecedenceOverExistingStoreDitchId() {
        val data = mapOf("photo1ImgId" to "12339")

        assertEquals(12355, PhotoImgIdResolver.resolve(12355, data, 1))
    }

    @Test
    fun existingStoreDitchIdIsPreservedWhenImportResponseOmitsId() {
        val data = mapOf("photo1ImgId" to "12339")

        assertEquals(12339, PhotoImgIdResolver.resolve(null, data, 1))
    }

    @Test
    fun missingResponseAndExistingIdRemainsMissing() {
        assertEquals(null, PhotoImgIdResolver.resolve(null, emptyMap(), 1))
    }
}
