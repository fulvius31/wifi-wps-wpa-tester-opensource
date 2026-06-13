package sangiorgi.wps.opensource.di

import android.content.Context
import android.net.wifi.WifiManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import sangiorgi.wps.lib.WpsConnectionManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WpsConnectionModule {

    @Provides
    @Singleton
    fun provideWpsConnectionManager(@ApplicationContext context: Context): WpsConnectionManager {
        return WpsConnectionManager(context)
    }

    @Provides
    @Singleton
    fun provideWifiManager(@ApplicationContext context: Context): WifiManager {
        return context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
}
