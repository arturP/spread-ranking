import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should handle empty orderbook response"
    
    request {
        method GET()
        urlPath "/market/orderbook/UNKNOWN_PAIR"
        headers {
            accept("application/json")
        }
    }
    
    response {
        status OK()
        headers {
            contentType("application/json")
        }
        body([
            ticker_id: "UNKNOWN_PAIR",
            bids: [],
            asks: [],
            timestamp: 1641234567890L
        ])
    }
}