{{/* Chart name */}}
{{- define "smartstock.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/* Release-qualified fullname */}}
{{- define "smartstock.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "smartstock.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/* Common labels applied to every object */}}
{{- define "smartstock.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
app.kubernetes.io/part-of: smartstock
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end -}}

{{/* Per-service selector labels. Call with (dict "svc" $name "ctx" $) */}}
{{- define "smartstock.selectorLabels" -}}
app.kubernetes.io/name: {{ .svc }}
app.kubernetes.io/instance: {{ .ctx.Release.Name }}
{{- end -}}

{{/* Secret name — existing (prod) or chart-rendered (dev) */}}
{{- define "smartstock.secretName" -}}
{{- if .Values.secrets.create -}}
{{- printf "%s-secrets" (include "smartstock.fullname" .) -}}
{{- else -}}
{{- required "secrets.existingSecret is required when secrets.create=false" .Values.secrets.existingSecret -}}
{{- end -}}
{{- end -}}

{{/* ServiceAccount name */}}
{{- define "smartstock.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (include "smartstock.fullname" .) .Values.serviceAccount.name -}}
{{- else -}}
{{- default "default" .Values.serviceAccount.name -}}
{{- end -}}
{{- end -}}

{{/* Fully-qualified image ref for a service. Call with (dict "svc" $name "ctx" $) */}}
{{- define "smartstock.image" -}}
{{- $g := .ctx.Values.global -}}
{{- printf "%s/%s/%s:%s" $g.registry $g.imageNamespace .svc $g.imageTag -}}
{{- end -}}
