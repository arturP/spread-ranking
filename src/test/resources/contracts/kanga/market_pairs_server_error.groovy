import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should handle server error when fetching market pairs"
    
    request {
        method GET()
        url "/market/pairs"
        headers {
            accept("application/json")
        }
    }
    
    response {
        status INTERNAL_SERVER_ERROR()
        headers {
            contentType("application/json")
        }
        body([
            error: "Internal server error",
            message: "Unable to process request"
        ])
    }
}