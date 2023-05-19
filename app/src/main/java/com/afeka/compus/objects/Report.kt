package com.afeka.compus.objects

import com.google.gson.annotations.SerializedName

class Report(
    @SerializedName("id") private val reportId: String,
    @SerializedName("reporter_email") private val reporterEmail: String,
    @SerializedName("text") private val text: String,
    @SerializedName("wp_id") private val wpId: String,
    @SerializedName("direction") private val direction: Int,
    @SerializedName("site_name") private val siteName: String) {

    fun getReportId() = reportId
    fun getReporterEmail() = reporterEmail
    fun getText() = text
    fun getWpId() = wpId
    fun getDirection() = direction
    fun getSiteName() = siteName

    fun isCompleteReport(): Boolean {

        return this.reporterEmail.isNotEmpty() &&
                this.text.isNotEmpty() &&
                this.wpId.isNotEmpty()

    }

    override fun toString(): String {
        return "Report: $reportId, $reporterEmail, $text, $wpId, $direction, $siteName"
    }
}