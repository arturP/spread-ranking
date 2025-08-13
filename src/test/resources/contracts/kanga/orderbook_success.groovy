import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should return orderbook successfully"
    
    request {
        method GET()
        urlPath "/market/orderbook/BTC_PLN"
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
            ticker_id: "BTC_PLN",
            bids: [
                ["185000.00", "0.1"],
                ["184500.00", "0.2"],
                ["184000.00", "0.5"]
            ],
            asks: [
                ["186000.00", "0.1"],
                ["186500.00", "0.2"], 
                ["187000.00", "0.3"]
            ],
            timestamp: 1641234567890L
        ])
    }
}