package tv.youi.kalturaplayertest.model;

public class WrapperBroadpeakConfig {

    String analyticsAddress = "https://analytics.kaltura.com/api_v3/index.php";
    String nanoCDNHost = "cdnapisec.kaltura.com";
    String broadpeakDomainNames = "*";


    public WrapperBroadpeakConfig() {}

    public String getAnalyticsAddress() {
        return analyticsAddress;
    }

    public String getNanoCDNHost() {
        return nanoCDNHost;
    }

    public String getBroadpeakDomainNames() {
        return broadpeakDomainNames;
    }
}
