package com.service

interface Gateway {
    fun send(userId: String, message: String)
}