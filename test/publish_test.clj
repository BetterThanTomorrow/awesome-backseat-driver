(ns publish-test
  (:require [clojure.test :refer [deftest is]]
            [publish :as pub]))

(deftest collect-publish-errors-ok
  (is (empty? (pub/collect-publish-errors
               {:branch "next"
                :clean? true
                :has-public? true
                :public-master-in-history? true
                :unreleased ["- Add skill"]}))))

(deftest collect-publish-errors-requires-public-master-in-history
  (is (some #(re-find #"public/master" %)
            (pub/collect-publish-errors
             {:branch "next"
              :clean? true
              :has-public? true
              :public-master-in-history? false
              :unreleased ["- Add skill"]}))))

(deftest changelog-notes-extracts-version-section
  (let [md (str "# Changelog\n\n"
                "## [Unreleased]\n\n"
                "## [1.0.16] - 2026-09-23\n\n"
                "- Ship it\n\n"
                "## [1.0.15] - 2026-06-06\n\n"
                "- Old\n")]
    (is (= "- Ship it" (pub/changelog-notes md "1.0.16")))))
