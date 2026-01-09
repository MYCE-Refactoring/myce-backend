package com.myce.restclient.service;

public interface RestClientService {
    <T> void send(String path, T body);
}
