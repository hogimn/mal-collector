package com.hogimn.malcollector.restsupport

class HttpServiceException(
    val statusCode: Int,
    val responseBody: String
) : RuntimeException("HTTP Request failed with status code: $statusCode")