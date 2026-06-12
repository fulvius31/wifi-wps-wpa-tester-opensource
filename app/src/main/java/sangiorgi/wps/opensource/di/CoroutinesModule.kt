package sangiorgi.wps.opensource.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifies the application-lifetime [CoroutineScope]. Use it for work that must outlive the
 * component that started it (e.g. cleanup in ViewModel.onCleared(), where viewModelScope is
 * already cancelled).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        // SupervisorJob so one failed cleanup never cancels the rest; IO for the blocking
        // native cleanup calls.
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
