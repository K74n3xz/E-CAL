package net.k74n3xz.ecal.core.preference

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.k74n3xz.ecal.core.preference.api.PreferenceRepository
import net.k74n3xz.ecal.core.preference.impl.DataStorePreferenceRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface PreferenceModule {
    @Binds
    fun bindPreferenceRepository(dataStorePreferenceRepository: DataStorePreferenceRepository): PreferenceRepository
}