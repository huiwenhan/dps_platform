/*
 *
 *  *   HORTONWORKS DATAPLANE SERVICE AND ITS CONSTITUENT SERVICES
 *  *
 *  *   (c) 2016-2018 Hortonworks, Inc. All rights reserved.
 *  *
 *  *   This code is provided to you pursuant to your written agreement with Hortonworks, which may be the terms of the
 *  *   Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 *  *   to distribute this code.  If you do not have a written agreement with Hortonworks or with an authorized and
 *  *   properly licensed third party, you do not have any rights to this code.
 *  *
 *  *   If this code is provided to you under the terms of the AGPLv3:
 *  *   (A) HORTONWORKS PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *  *   (B) HORTONWORKS DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 *  *     LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *  *   (C) HORTONWORKS IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 *  *     FROM OR RELATED TO THE CODE; AND
 *  *   (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, HORTONWORKS IS NOT LIABLE FOR ANY
 *  *     DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED TO,
 *  *     DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR UNAVAILABILITY,
 *  *     OR LOSS OR CORRUPTION OF DATA.
 *
 */
package com.hortonworks.dataplane.gateway.service;

import com.hortonworks.dataplane.gateway.domain.PluginManifest;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PluginManifestService {

  private static final Logger logger = LoggerFactory.getLogger(PluginManifestService.class);

  @Autowired
  PluginManifestInterface pluginInterface;

  private static ConcurrentMap<String, PluginManifest> manifests = new ConcurrentHashMap<>();

  public Optional<PluginManifest> retrieve(String serviceName) {
    if(manifests.isEmpty()) {
      List<PluginManifest> fManifests = pluginInterface.list();

      fManifests.stream().forEach(cManifest -> {
        logger.info("Registering serviceId '" + cManifest.getName() + "' with manifest: " + cManifest.toString());
        manifests.put(cManifest.getName(), cManifest);
      });
    }
    return Optional.ofNullable(manifests.get(serviceName));
  }

  public boolean isAuthorized(String serviceId, String[] roles) {
    Optional<PluginManifest> oPlugin = this.retrieve(serviceId);
    if(!oPlugin.isPresent()) {
      return false;
    }

    PluginManifest plugin = oPlugin.get();
    return plugin.getRequirePlatformRoles().isEmpty() || !CollectionUtils.intersection(plugin.getRequirePlatformRoles(), Arrays.asList(roles)).isEmpty();
  }

}
