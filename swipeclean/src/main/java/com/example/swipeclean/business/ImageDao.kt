package com.example.swipeclean.business

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.swipeclean.model.Image
import com.example.swipeclean.other.Constants.IMAGE_DELETE
import com.example.swipeclean.other.Constants.IMAGE_KEEP

@Dao
interface ImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertImage(image: Image)

    @Query("SELECT sourceId FROM image WHERE tag = $IMAGE_DELETE")
    fun getDeleteImageIds(): List<Long>

    @Query("SELECT sourceId FROM image WHERE tag = $IMAGE_KEEP")
    fun getKeepImageIds(): List<Long>

    @Query("DELETE FROM image WHERE sourceId = :sourceId")
    fun delete(sourceId: Long)

    @Query("DELETE FROM image WHERE sourceId IN (:sourceIds)")
    fun delete(sourceIds: List<Long>)

    @Query("UPDATE image SET tag = $IMAGE_KEEP WHERE sourceId = :sourceId")
    fun convertDeleteToKeep(sourceId: Long)

    @Query("UPDATE image SET tag = $IMAGE_KEEP WHERE sourceId in (:sourceIds)")
    fun convertDeleteToKeep(sourceIds: List<Long>)
}