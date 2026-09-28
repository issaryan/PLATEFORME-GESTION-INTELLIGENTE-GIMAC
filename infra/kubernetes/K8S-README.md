# JHipster-generated Kubernetes configuration

## Preparation

You will need to push your image to a registry. If you have not done so, use the following commands to tag and push the images:

```
$ docker image tag gateway registre.gimac.local/des/gateway
$ docker push registre.gimac.local/des/gateway
$ docker image tag iam registre.gimac.local/des/iam
$ docker push registre.gimac.local/des/iam
$ docker image tag audit registre.gimac.local/des/audit
$ docker push registre.gimac.local/des/audit
$ docker image tag document registre.gimac.local/des/document
$ docker push registre.gimac.local/des/document
$ docker image tag notification registre.gimac.local/des/notification
$ docker push registre.gimac.local/des/notification
$ docker image tag participant registre.gimac.local/des/participant
$ docker push registre.gimac.local/des/participant
$ docker image tag homologation registre.gimac.local/des/homologation
$ docker push registre.gimac.local/des/homologation
$ docker image tag certification registre.gimac.local/des/certification
$ docker push registre.gimac.local/des/certification
$ docker image tag incident registre.gimac.local/des/incident
$ docker push registre.gimac.local/des/incident
$ docker image tag monitoring registre.gimac.local/des/monitoring
$ docker push registre.gimac.local/des/monitoring
$ docker image tag rapprochement registre.gimac.local/des/rapprochement
$ docker push registre.gimac.local/des/rapprochement
$ docker image tag pilotage registre.gimac.local/des/pilotage
$ docker push registre.gimac.local/des/pilotage
$ docker image tag reporting registre.gimac.local/des/reporting
$ docker push registre.gimac.local/des/reporting
$ docker image tag collaboration registre.gimac.local/des/collaboration
$ docker push registre.gimac.local/des/collaboration
```

## Deployment

You can deploy all your apps by running the below bash command:

```
./kubectl-apply.sh -f (default option)  [or] ./kubectl-apply.sh -k (kustomize option) [or] ./kubectl-apply.sh -s (skaffold run)
```

If you want to apply kustomize manifest directly using kubectl, then run

```
kubectl apply -k ./
```

If you want to deploy using skaffold binary, then run

```
skaffold run [or] skaffold deploy
```

## Exploring your services

Use these commands to find your application's IP addresses:

```
$ kubectl get svc gateway -n gimac-des
```

## Scaling your deployments

You can scale your apps using:

```
kubectl scale deployment <app-name> --replicas <replica-count> -n gimac-des
```

## Zero-downtime deployments

The default way to update a running app in kubernetes, is to deploy a new image tag to your docker registry and then deploy it using:

```
kubectl set image deployment/<app-name>-app <app-name>=<new-image>  -n gimac-des
```

Using livenessProbes and readinessProbe allow you to tell Kubernetes about the state of your applications, in order to ensure availability of your services. You will need a minimum of two replicas for every application deployment if you want to have zero-downtime.
This is because the rolling upgrade strategy first stops a running replica in order to place a new. Running only one replica, will cause a short downtime during upgrades.

## Monitoring tools

### Prometheus metrics

Generator is also packaged with [Prometheus operator by CoreOS](https://github.com/coreos/prometheus-operator).

**Hint**: use must build your apps with `prometheus` profile active!

Application metrics can be explored in Prometheus through:

```
kubectl get svc jhipster-prometheus -n gimac-des
```

Also the visualisation can be explored in Grafana which is pre-configured with a dashboard view. You can find the service details by running:

```
kubectl get svc jhipster-grafana -n gimac-des
```

- If you have chosen _Ingress_, then you should be able to access Grafana using the given ingress domain.
- If you have chosen _NodePort_, then point your browser to an IP of any of your nodes and use the node port described in the output.
- If you have chosen _LoadBalancer_, then use the IaaS provided load balancer IP

## Keycloak

The following Keycloak [best practices](https://www.keycloak.org/server/configuration-production) for production are included in the generated k8s: a production grade database PostgreSQL, the `edge` deployment mode, high availability through clustered deployment, and `HOSTNAME` configuration. HTTP/TLS is not enabled for ease of deployment, but it is required for a secure exchange of credentials and other sensitive data with Keycloak, preventing several attack vectors.

If you are using minikube, install the ingress controller for [cloud deployment](https://kubernetes.github.io/ingress-nginx/deploy/#gce-gke) (the minikube ingress add on does not work on macOS or Windows).

```
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.7.0/deploy/static/provider/cloud/deploy.yaml
```

Then run `minikube tunnel` in a separate terminal window.
Get the `ingress-nginx-controller` external IP:

```
kubectl get svc/ingress-nginx-controller -n ingress-nginx
```

The output will look like the following:

```
NAME                       TYPE           CLUSTER-IP      EXTERNAL-IP     PORT(S)                      AGE
ingress-nginx-controller   LoadBalancer   10.103.44.162   10.103.44.162   80:30982/TCP,443:32664/TCP   27s
```

When running the `jhipster k8s` generator, it will prompt for a root FQDN (fully qualified domain name). You can use `nip.io` as the DNS provider and set `<external-ip>.nip.io`.

## Troubleshooting

> my app doesn't get pulled, because of 'imagePullBackof'

Check the docker registry your Kubernetes cluster is accessing. If you are using a private registry, you should add it to your namespace by `kubectl create secret docker-registry` (check the [docs](https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/) for more info)

> my applications are stopped, before they can boot up

This can occur if your cluster has low resource (e.g. Minikube). Increase the `initialDelaySeconds` value of livenessProbe of your deployments

> my applications are starting very slow, despite I have a cluster with many resources

The default setting are optimized for middle-scale clusters. You are free to increase the JAVA_OPTS environment variable, and resource requests and limits to improve the performance. Be careful!
