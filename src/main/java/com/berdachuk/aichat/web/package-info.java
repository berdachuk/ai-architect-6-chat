@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"core", "chat :: service", "chat :: domain", "llm"})
package com.berdachuk.aichat.web;
