/*
 * Copyright 2016 the original author or authors.
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
package com.greglturnquist;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Greg Turnquist
 */
@RestController
public class KeyController {

	private static final Logger log = LoggerFactory.getLogger(KeyController.class);

	@Autowired
	StringRedisTemplate redisTemplate;

	@Autowired
	ObjectMapper mapper;

	@RequestMapping(method = RequestMethod.GET, value = "/keys", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getKeys() {
		Set<String> keys = redisTemplate.keys("*");

		return ResponseEntity.ok(new Resources<>(
			Collections.EMPTY_LIST,
			keys.stream()
				.map((String key) -> linkTo(methodOn(KeyController.class).getKey(key)).withRel(key))
				.collect(Collectors.toList())
		));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/key", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getKey(@RequestParam("q") String q) {
		final Set<String> keys = redisTemplate.keys(q);

		if (keys.size() > 1) {

			return ResponseEntity.notFound().build();

		} else if (keys.size() == 1) {

			final String content = redisTemplate.opsForValue().get(q);

			if (content == null) {
				return ResponseEntity.notFound().build();
			}

			log.info("Processing " + content);

			List<Link> links = new ArrayList<>();
			links.add(linkTo(methodOn(KeyController.class).getKey(q)).withSelfRel());
			links.add(linkTo(methodOn(KeyController.class).getKeys()).withRel("root"));

			Object value = content;

			if (content.startsWith("[")) {
				try {
					List<?> vals = mapper.readValue(content, List.class);
					for (Object val : vals) {
						if (val instanceof String) {
							String stringVal = (String) val;
							log.info("Is " + stringVal + " a key?");
							final Set<String> valKeys = redisTemplate.keys(stringVal);
							if (valKeys.size() > 1) {
								log.info("Exact key? YES");
								for (String key : valKeys) {
									links.add(linkTo(methodOn(KeyController.class).getKey(key)).withRel(key));
								}
							} else {
								final Set<String> wildcardKeys = redisTemplate.keys("*" + stringVal + "*");
								for (String key : wildcardKeys) {
									links.add(linkTo(methodOn(KeyController.class).getKey(key)).withRel(key));
								}
							}
						}
					}
					return ResponseEntity.ok(new Resources<>(
						vals,
						links
					));
				} catch (IOException e) {
					// Swallow exception and move on
				}
				return ResponseEntity.ok(value);
			} else if (content.startsWith("{")) {
				try {
					value = mapper.readValue(content, Map.class);
					return ResponseEntity.ok(new Resource<>(
						value,
						links
					));
				} catch (IOException e) {
					// Swallow exception and move on
				}
				return ResponseEntity.ok(value);

			} else {

				return ResponseEntity.ok(value);

			}


		} else { // keys.size() == 0

			return ResponseEntity.notFound().build();

		}
	}

}
