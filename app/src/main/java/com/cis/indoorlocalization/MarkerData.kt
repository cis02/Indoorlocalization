package com.cis.indoorlocalization

import android.graphics.PointF
import android.os.Parcel
import android.os.Parcelable

data class MarkerData(
    val position: PointF,
    var name: String = "",
    var wifiData: String = "" // Placeholder for future Wi-Fi data
) : Parcelable {

    constructor(parcel: Parcel) : this(
        PointF(parcel.readFloat(), parcel.readFloat()),
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloat(position.x)
        parcel.writeFloat(position.y)
        parcel.writeString(name)
        parcel.writeString(wifiData)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MarkerData> {
        override fun createFromParcel(parcel: Parcel): MarkerData {
            return MarkerData(parcel)
        }

        override fun newArray(size: Int): Array<MarkerData?> {
            return arrayOfNulls(size)
        }
    }
}
