import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should handle not found error for invalid market"
    
    request {
        method GET()
        urlPath "/market/orderbook/INVALID_PAIR"
        headers {
            accept("application/json")
        }
    }
    
    response {
        status NOT_FOUND()
        headers {
            contentType("application/json")
        }
        body([
            error: "Market not found",
            message: "The requested market pair does not exist"
        ])
    }
}