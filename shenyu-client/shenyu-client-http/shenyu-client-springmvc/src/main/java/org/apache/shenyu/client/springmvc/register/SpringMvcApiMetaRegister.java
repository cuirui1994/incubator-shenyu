/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shenyu.client.springmvc.register;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.shenyu.client.core.disruptor.ShenyuClientRegisterEventPublisher;
import org.apache.shenyu.client.core.register.ApiBean;
import org.apache.shenyu.client.core.register.ClientRegisterConfig;
import org.apache.shenyu.client.core.register.matcher.AnnotatedApiDefinitionMatcher;
import org.apache.shenyu.client.core.register.matcher.Matcher;
import org.apache.shenyu.client.core.register.registrar.AbstractApiMetaRegistrar;
import org.apache.shenyu.client.springmvc.annotation.ShenyuSpringMvcClient;
import org.apache.shenyu.common.enums.RpcTypeEnum;
import org.apache.shenyu.common.utils.PathUtils;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.List;
import java.util.Objects;

public class SpringMvcApiMetaRegister extends AbstractApiMetaRegistrar {

    private final Matcher<ApiBean.ApiDefinition> apiDefinitionMatcher =
            new AnnotatedApiDefinitionMatcher(ShenyuSpringMvcClient.class)
                    .or(api -> AnnotationUtils.isAnnotationDeclaredLocally(ShenyuSpringMvcClient.class, api.getBeanClass()));

    private final Boolean addPrefixed;

    private final String appName;

    private final String host;

    private final Integer port;

    public SpringMvcApiMetaRegister(final ShenyuClientRegisterEventPublisher publisher,
                                    final ClientRegisterConfig clientRegisterConfig) {
        super(publisher);

        this.addPrefixed = clientRegisterConfig.getAddPrefixed();
        this.appName = clientRegisterConfig.getAppName();
        this.host = clientRegisterConfig.getHost();
        this.port = clientRegisterConfig.getPort();
    }

    @Override
    protected Boolean preMatch(final ApiBean apiBean) {

        ShenyuSpringMvcClient annotation = apiBean.getAnnotation(ShenyuSpringMvcClient.class);

        return annotation != null && annotation.path().endsWith("/**");
    }

    @Override
    protected MetaDataRegisterDTO preParse(final ApiBean apiBean) {

        ShenyuSpringMvcClient annotation = apiBean.getAnnotation(ShenyuSpringMvcClient.class);
        String apiPath = PathUtils.pathJoin(apiBean.getContextPath(), annotation.path());

        return MetaDataRegisterDTO.builder()
                .contextPath(apiBean.getContextPath())
                .addPrefixed(addPrefixed)
                .appName(appName)
                .serviceName(apiBean.getBeanClass().getName())
                .host(host)
                .port(port)
                .methodName(null)
                .path(apiPath)
                .pathDesc(annotation.desc())
                .parameterTypes(null)
                .rpcType(RpcTypeEnum.HTTP.getName())
                .enabled(annotation.enabled())
                .ruleName(StringUtils.defaultIfBlank(annotation.ruleName(), apiPath))
                .registerMetaData(annotation.registerMetaData())
                .build();
    }

    @Override
    protected Boolean match(final ApiBean apiBean) {
        ShenyuSpringMvcClient annotation = apiBean.getAnnotation(ShenyuSpringMvcClient.class);
        if (annotation != null) {
            return !annotation.path().endsWith("/**");
        }
        return true;
    }

    @Override
    protected Boolean match(final ApiBean.ApiDefinition apiDefinition) {
        return apiDefinitionMatcher.match(apiDefinition);
    }

    @Override
    protected List<MetaDataRegisterDTO> parse(final ApiBean.ApiDefinition apiDefinition) {

        ShenyuSpringMvcClient methodAnnotation = apiDefinition.getAnnotation(ShenyuSpringMvcClient.class);

        String methodPath = Objects.isNull(methodAnnotation) ? StringUtils.EMPTY : methodAnnotation.path();

        if (StringUtils.isEmpty(methodPath)) {
            methodPath = apiDefinition.getMethodPath();
        }

        String apiPath = PathUtils.pathJoin(apiDefinition.getContextPath(), apiDefinition.getBeanPath(), methodPath);

        ShenyuSpringMvcClient classAnnotation = apiDefinition.getApiBean()
                .getAnnotation(ShenyuSpringMvcClient.class);

        String pathDesc = Objects.isNull(methodAnnotation) ? classAnnotation.desc() : methodAnnotation.desc();

        boolean enabled = (Objects.isNull(classAnnotation) || classAnnotation.enabled())
                && (Objects.isNull(methodAnnotation) || methodAnnotation.enabled());

        String ruleName = Objects.isNull(methodAnnotation) || StringUtils.isEmpty(methodAnnotation.ruleName())
                ? apiPath : methodAnnotation.ruleName();

        boolean registerMetaData = (Objects.isNull(classAnnotation) || classAnnotation.registerMetaData())
                && (Objects.isNull(methodAnnotation) || methodAnnotation.registerMetaData());

        return Lists.newArrayList(MetaDataRegisterDTO.builder()
                .contextPath(apiDefinition.getContextPath())
                .addPrefixed(addPrefixed)
                .appName(appName)
                .host(host)
                .port(port)
                .serviceName(apiDefinition.getBeanClass().getName())
                .methodName(apiDefinition.getApiMethodName())
                .path(apiPath)
                .pathDesc(pathDesc)
                .parameterTypes(apiDefinition.getParameterTypes())
                .rpcType(RpcTypeEnum.HTTP.getName())
                .enabled(enabled)
                .ruleName(ruleName)
                .registerMetaData(registerMetaData)
                .build());
    }
}
