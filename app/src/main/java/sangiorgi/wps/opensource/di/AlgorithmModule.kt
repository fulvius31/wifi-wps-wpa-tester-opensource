package sangiorgi.wps.opensource.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import sangiorgi.wps.opensource.algorithm.strategy.AlgorithmFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AlgorithmModule {

    @Provides
    @Singleton
    fun provideAlgorithmFactory(@ApplicationContext context: Context): AlgorithmFactory {
        return AlgorithmFactory(context)
    }
}
