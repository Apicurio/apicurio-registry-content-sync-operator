/*
 * Copyright 2021 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.sync.controller;

/**
 * @author Fabian Martinez
 */
public class OperationContext<T> {

    private T data;

    private String error;

    public OperationContext(String error) {
        this.error = error;
    }

    public OperationContext(T data) {
        this.data = data;
    }

    public static <T> OperationContext<T> with(T data) {
        return new OperationContext<>(data);
    }

    public static <T> OperationContext<T> error(String err) {
        return new OperationContext<>(err);
    }

    /**
     * @return the data
     */
    public T getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(T data) {
        this.data = data;
    }

    /**
     * @return the error
     */
    public String getError() {
        return error;
    }

    /**
     * @param error the error to set
     */
    public void setError(String error) {
        this.error = error;
    }

}
