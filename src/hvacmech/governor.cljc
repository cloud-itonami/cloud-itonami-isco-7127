(ns hvacmech.governor
  "HvacMechGovernor — the independent safety/scope layer gating every
  service scheduling/logistics proposal an advisor may make for an AC
  and refrigeration mechanic crew. The governor never dispatches
  hardware itself, never performs refrigeration-system service, and
  never finalizes a refrigerant-system-service-execution decision
  (e.g. deciding to proceed with a specific refrigerant-system
  service) or overrides/bypasses a technician-certification
  requirement — those are permanently out of this actor's scope and
  remain a certified technician's exclusive judgment (README's
  'Robotics premise': this actor coordinates SERVICE
  SCHEDULING/LOGISTICS ONLY — it never performs refrigeration-system
  service itself). Modeled on cloud-itonami-isco-7111's
  housebuilder.governor (closest domain shape, itself modeled on
  cloud-itonami-isco-3313's accountingsupport.governor and
  cloud-itonami-isco-9311's mininglabor.governor for the
  physical-safety-domain shape).

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. technician provenance — the technician must be independently
                                verified/registered (including
                                certification status) before any
                                action.
    2. service-account provenance — the service account must be
                                independently verified/registered
                                before any action.
    3. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never performs refrigeration-system
                                service itself; it only gates what the
                                advisor may coordinate).
    4. closed op-allowlist    — only :log-service-record,
                                :schedule-service-operation,
                                :flag-safety-concern and
                                :coordinate-supply-order may ever be
                                proposed; anything else is refused.
    5. scope-excluded action  — any proposal to directly finalize a
                                refrigerant-system-service-execution
                                decision (e.g. deciding to proceed
                                with a specific refrigerant-system
                                service), or to override or bypass a
                                technician-certification requirement,
                                is a hard, permanent block (checked
                                both against the proposed :op and,
                                defense-in-depth, against the
                                proposal's :rationale text — matched as
                                full finalization/execution ACTION
                                phrases such as \"proceed with the
                                refrigerant-system service\" /
                                \"authorize the refrigerant-system
                                service execution\" / \"override the
                                technician-certification requirement\",
                                never as bare nouns like \"refrigerant\",
                                \"certification\" or \"technician\", so
                                the check can never self-trip on the
                                advisor's own routine rationale text,
                                e.g. \"logged service record for
                                technician …\" or \"scheduled service
                                operation for refrigerant diagnostic
                                task …\" or \"…routed for certified
                                technician review\" — all three
                                legitimately contain those bare nouns
                                but none is a finalization action, and
                                all are exercised by
                                `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off
  regardless of confidence):
    6. :op :flag-safety-concern (a refrigerant-leak / pressure-anomaly
                                / electrical-hazard concern always
                                escalates to a human, never
                                auto-commits).
    7. :op :coordinate-supply-order above `supply-cost-threshold`.
    8. low confidence (< `confidence-floor`)."
  (:require [clojure.string :as str]
            [hvacmech.store :as store]))

(def confidence-floor 0.6)
(def supply-cost-threshold 2000)

(def allowed-ops
  #{:log-service-record :schedule-service-operation
    :flag-safety-concern :coordinate-supply-order})

;; Defense-in-depth: none of these ops are ever in `allowed-ops`
;; above, so they are already refused by the closed-allowlist check
;; below; they are named again here — as explicit finalization/
;; execution ACTIONS, never bare nouns — so a future allowlist edit
;; cannot silently re-open this specific out-of-scope path without
;; also touching this list.
(def ^:private scope-excluded-ops
  #{:finalize-refrigerant-service-decision
    :authorize-refrigerant-system-service-execution
    :proceed-with-refrigerant-system-service
    :override-technician-certification-requirement
    :bypass-technician-certification-requirement})

;; Full finalization/execution ACTION phrases only — never bare nouns
;; ("refrigerant", "certification", "technician", "pressure",
;; "electrical") — so this can never match inside the mock advisor's
;; own default rationale text (which legitimately contains those bare
;; nouns, e.g. "refrigerant diagnostic task" / "certified technician
;; review"). See
;; `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`.
(def ^:private scope-excluded-phrases
  ["proceed with the refrigerant-system service"
   "proceed with the refrigerant system service"
   "finalize the refrigerant-system service execution decision"
   "finalize the refrigerant system service execution decision"
   "authorize the refrigerant-system service execution"
   "authorize the refrigerant system service execution"
   "override the technician-certification requirement"
   "override the technician certification requirement"
   "bypass the technician-certification requirement"
   "bypass the technician certification requirement"])

(defn- contains-excluded-phrase? [s]
  (let [s (str/lower-case (or s ""))]
    (boolean (some #(str/includes? s %) scope-excluded-phrases))))

(defn- hard-violations [proposal technician-record service-account-record]
  (let [{:keys [op rationale]} proposal]
    (cond-> []
      (nil? technician-record)
      (conj {:rule :no-technician
             :detail "未登録 technician への提案は不可（technician record は独立して検証・登録済み — certification status を含む — でなければならない）"})

      (nil? service-account-record)
      (conj {:rule :no-service-account
             :detail "未登録 service account への提案は不可（service account record は独立して検証・登録済みでなければならない）"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor は refrigeration-system service を直接実行しない）"})

      (not (contains? allowed-ops op))
      (conj {:rule :unknown-op
             :detail (str op " は closed op-allowlist に無い — 提案不可")})

      (or (contains? scope-excluded-ops op) (contains-excluded-phrase? rationale))
      (conj {:rule :scope-excluded-action
             :detail "冷媒系統サービスの実行判断の確定・technician certification 要件の上書き/回避は、この actor の権限外 — 常に永続ブロック"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `hvacmech.store/Store`. Pure — never mutates
  the store, never dispatches a service operation."
  [request _context proposal store]
  (let [technician-record (store/technician store (:technician-id request))
        service-account-record (some->> (:service-account-id proposal) (store/service-account store))
        hard (hard-violations proposal technician-record service-account-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        supply-order-over-threshold?
        (and (= :coordinate-supply-order (:op proposal))
             (number? (:cost proposal))
             (> (:cost proposal) supply-cost-threshold))
        always-risky? (or (= :flag-safety-concern (:op proposal))
                           supply-order-over-threshold?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
