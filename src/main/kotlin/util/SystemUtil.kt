package util

// company managed windows laptop --> use Windows truststore
fun trustStoreMagic() {
    if ("KRAMP".equals(System.getenv("USERDOMAIN")) && "Windows_NT".equals(System.getenv("OS"))) {
        System.setProperty("javax.net.ssl.trustStore", "NONE")
        System.setProperty("javax.net.ssl.trustStoreType", "Windows-ROOT")
    }
}