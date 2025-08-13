import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should return market pairs successfully"
    
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
        body([
            [
                ticker_id: "BTC_PLN",
                base: "BTC", 
                target: "PLN"
            ],
            [
                ticker_id: "ETH_PLN",
                base: "ETH",
                target: "PLN"
            ],
            [
                ticker_id: "LTC_PLN", 
                base: "LTC",
                target: "PLN"
            ]
        ])
    }
}