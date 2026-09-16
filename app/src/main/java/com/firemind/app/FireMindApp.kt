package com.firemind.app

import android.app.Application
import com.firemind.app.ai.FireMindClient
import com.firemind.app.data.CatalogRepository
import com.firemind.app.data.WatchlistStore

/** App-scoped dependencies; created once, shared across screens. */
class FireMindApp : Application() {

    lateinit var catalogRepository: CatalogRepository
        private set
    lateinit var watchlistStore: WatchlistStore
        private set
    lateinit var fireMindClient: FireMindClient
        private set

    override fun onCreate() {
        super.onCreate()
        catalogRepository = CatalogRepository(this)
        watchlistStore = WatchlistStore(this)
        fireMindClient = FireMindClient(BuildConfig.BACKEND_URL)
    }
}
