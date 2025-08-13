import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should handle empty market pairs response"
    
    request {
        method GET()
        url "/market/pairs"
        headers {
            accept("application/json")
        }
    }
    
    response {
        status OK()
        headers {
            contentType("application/json")
        }
        body([])
    }
}