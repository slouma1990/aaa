version = 1

cloudstream {
    language = "ar"
    description = "مشاهدة وتحميل الانمي المترجم اون لاين"
    authors = listOf("YourName")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     **/
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "Anime"
    )

    iconUrl = "https://anime4up.rest/wp-content/uploads/2019/03/Anime4up-Icon-1.png"
}

android {
    defaultConfig {
        minSdk = 21
    }
}